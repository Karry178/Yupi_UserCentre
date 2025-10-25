package com.yupi.usercentre.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.usercentre.model.domain.User;
import com.yupi.usercentre.model.domain.UserLocation;
import com.yupi.usercentre.model.domain.UserTeam;
import com.yupi.usercentre.model.vo.TeamMemberDistanceVO;
import com.yupi.usercentre.model.vo.UserLocationVO;
import com.yupi.usercentre.service.UserLocationService;
import com.yupi.usercentre.mapper.UserLocationMapper;
import com.yupi.usercentre.service.UserService;
import com.yupi.usercentre.service.UserTeamService;
import com.yupi.usercentre.utils.GeoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author 17832
* @description 针对表【user_location(用户位置表)】的数据库操作Service实现
* @createDate 2025-10-20 18:34:52
*/
@Service
@Slf4j
public class UserLocationServiceImpl extends ServiceImpl<UserLocationMapper, UserLocation>
    implements UserLocationService{

    // 引入UserTeamService层
    @Resource
    private UserTeamService userTeamService;

    // 引入UserService层
    @Resource
    private UserService userService;


    /**
     * 查询队伍成员位置,并返回列表
     *
     * @param teamId      队伍id
     * @param maxDistance 两点经纬度间隔最大值
     * @param userId          用户id
     * @return 队伍成员位置列表
     *
     *
     * 完整流程（7大步骤）：
     *
     * 1. 参数校验
     *    └─ 验证 teamId 和 userId 是否有效
     *
     * 2. 查询队伍所有成员ID
     *    └─ 从 user_team 表查询该队伍的所有成员
     *
     * 3. 获取当前登录用户的位置
     *    └─ 查询当前登录用户的经纬度坐标
     *
     * 4. 批量查询成员位置信息
     *    └─ 根据成员ID列表，批量查询位置信息
     *
     * 5. 计算距离并构建VO对象
     *    └─ 遍历成员，计算距离，转换成VO
     *
     * 6. 按距离排序
     *    └─ 使用 Stream API 排序
     *
     * 7. 返回结果
     *    └─ 返回排序后的列表
     */
    @Override
    public List<TeamMemberDistanceVO> getTeamMembersLocation(Long teamId, Double maxDistance, Long userId) {

        // ========== 步骤1：参数校验 ==========
        // 校验teamId和userId
        if (teamId == null || teamId <= 0) {
            log.info("队伍Id不合法，teamId:{}", teamId);
            // 【声明】Service层通常返回日志信息，而不是抛出异常；通常是Controller层做异常判断！
            return Collections.emptyList(); // 返回不可变的空列表
        }

        if (userId == null || userId <= 0) {
            log.info("用户Id不合法，userId:{}", userId);
            return Collections.emptyList();
        }

        // ========== 步骤2：查询队伍所有成员ID ==========
        // 2.1 构建查询条件
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId", teamId);
        queryWrapper.select("userId"); // 只查询用户ID这一列

        // 2.2 执行查询条件，并返回对应的 用户队伍 列表
        List<UserTeam> userTeamList = userTeamService.list(queryWrapper);

        // 2.3 判断队伍是否为空
        if (userTeamList.isEmpty()) {
            log.info("当前队伍{}没有成员", teamId);
            return Collections.emptyList();
        }

        // 2.4 使用stream流的map()方法 提取用户ID列表 -> 目的是：批量查询队伍中所有人的位置信息
        List<Long> memberIds = userTeamList.stream().map(UserTeam::getUserId)
                .collect(Collectors.toList());

        // ========== 步骤3：获取当前用户的位置 ==========
        // 3.1 查询当前用户的位置信息
        UserLocation loginUserLocation = this.getById(userId);

        // 3.2 判断当前登录用户是否设置了位置
        if (loginUserLocation == null) {
            log.info("当前登录用户{}未设置位置信息", userId);
            return Collections.emptyList();
        }

        // 3.3 获取当前登录用户的经纬度坐标
        Double loginUserLon = loginUserLocation.getLongitude();
        Double loginUserLat = loginUserLocation.getLatitude();

        // 3.4 验证坐标有效性
        if (!GeoUtils.isValidCoordinate(loginUserLon, loginUserLat)) {
            log.info("当前队伍{}成员位置信息不完整", teamId);
            return Collections.emptyList();
        }


        // ========== 步骤4：批量查询成员位置信息 ==========
        // 4.1 构建查询条件
        QueryWrapper<UserLocation> locationQueryWrapper = new QueryWrapper<>();
        locationQueryWrapper.in("userId", memberIds); // 查询条件：userId要在memberIds中

        // 4.2 执行查询条件，并返回对应的 用户位置 列表
        List<UserLocation> memberLocationList = this.list(locationQueryWrapper);

        // 4.3 判断是否有成员位置信息
        if (memberLocationList.isEmpty()) {
            log.info("当前队伍{}中没有成员", teamId);
            return Collections.emptyList();
        }


        // ========== 步骤5：批量查询用户基本信息 ==========
        // 5.1 提取所有 有位置信息的用户Id -> stream流
        // 为什么map()方法提取userId，可以保证提取到的用户都有位置信息呢？上面的查询条件为什么可以保证查找的成员位置信息都是非空的？
        // 因为在上面的查询条件中，没有信息的不会被加入memberLocationList中
        List<Long> userIdWithLocation = memberLocationList.stream()
                .map(UserLocation::getUserId)
                .collect(Collectors.toList());

        // 5.2 批量查询用户信息
        List<User> userList = userService.listByIds(userIdWithLocation);

        // 5.3 构建用户Id到用户对象的映射(性能优化) -> 创建一个Map，将用户Id映射到用户对象
        // 目的：将List<User>转为Map<userId, User> -> 表结构查询速度优于列表
        Map<Long, User> userMap = userList.stream().collect(Collectors.toMap(User::getId, user -> user));


        // ========== 步骤6：计算距离并构建VO对象 ==========
        // 6.1 遍历成员位置，计算距离
        ArrayList<TeamMemberDistanceVO> result = new ArrayList<>();
          // 循环遍历成员位置列表信息
        for (UserLocation memberLocation : memberLocationList) {
            // 6.2 跳过当前用户自己
            if (memberLocation.getUserId().equals(userId)) {
                continue;
            }

            // 6.3 获取成员的经纬度
            Double memberLon = memberLocation.getLongitude();
            Double memberLat = memberLocation.getLatitude();

            // 6.4 验证成员经纬度有效性
            if (!GeoUtils.isValidCoordinate(memberLon, memberLat)) {
                log.info("用户{}的位置信息不完整，跳过", memberLocation.getUserId());
                continue;
            }

            // 6.5 计算队伍每个成员与登录用户的距离(调用GeoUtils)
            double distance = GeoUtils.calculateDistance(loginUserLon, loginUserLat, memberLon, memberLat);

            // 6.6 获取除自己外的队伍用户基本信息
            User user = userMap.get(memberLocation.getUserId()); // 先从Map快速获取其余人的用户信息
            if (user == null) {
                log.info("用户{}的信息不存在，跳过", memberLocation.getUserId());
                continue;
            }

            // 6.7 构建VO对象给前端
            TeamMemberDistanceVO teamMemberDistanceVO = new TeamMemberDistanceVO();
            teamMemberDistanceVO.setUserId(memberLocation.getUserId()); // 设置用户Id
            teamMemberDistanceVO.setCity(memberLocation.getCity()); // 设置用户所在城市
            teamMemberDistanceVO.setProvince(memberLocation.getProvince()); // 设置用户所在省份
            teamMemberDistanceVO.setDistance(distance); // 设置登录用户与其余用户的距离

            // 6.8 添加新的VO对象到结果列表中
            result.add(teamMemberDistanceVO);
        }

        // ========== 步骤7：按距离排序 ==========
        // 7.1 使用stream流排序 -> 根据距离升序排序(由小到大)
        result.sort(Comparator.comparing(TeamMemberDistanceVO::getDistance));

        // ========== 步骤8：返回结果 ==========
        log.info("队伍{}共查询到{}个成员的位置", teamId, result.size());
        return result;
    }


    /**
     * 更新用户位置
     * @param userLocation 用户位置信息
     * @return 更新后的用户位置信息
     */
    @Override
    public UserLocation updatedLocation(UserLocation userLocation) {

        // 1.参数校验 -> 验证userLocation、userId和用户经纬度是否有效
        if (userLocation == null || userLocation.getUserId() == null) {
            log.info("用户位置参数{}不正确", userLocation);
        }

        if (!GeoUtils.isValidCoordinate(userLocation.getLongitude(), userLocation.getLatitude ())) {
            log.info("登录用户的位置设置不正确", userLocation);
        }

        // 2.查询用户是否已有位置信息
        QueryWrapper<UserLocation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userLocation.getUserId());
          // 2.1 执行查询条件
            /**
             * 为什么使用this.getOne()方法？
             *
             * getOne() 专门用于查询单条记录
             * - 输入：QueryWrapper（查询条件）
             * - 输出：单个对象（不是 List）
             * - 如果没有记录，返回 null
             * - 如果有多条记录，返回第一条（或抛异常，取决于配置）
              */
        UserLocation existingLocation = this.getOne(queryWrapper);

        // 3.拿到位置信息后，判断是否已有位置信息 -> 存在则进行更新操作；不存在则进行插入操作
        if (existingLocation != null) {
            // 更新位置信息
            existingLocation.setLongitude(userLocation.getLongitude());
            existingLocation.setLatitude(userLocation.getLatitude());
            userLocation.setUpdateTime(new Date());
            this.updateById(existingLocation); // 通过Service层更新位置信息
        } else {
            // 新增位置信息
            userLocation.setCreateTime(new Date()); // 设置创建时间
            userLocation.setUpdateTime(new Date()); // 设置更新时间
            // 为什么新增位置信息，不去添加经纬度呢？-> 因为经纬度等信息已经在userLocation中了
            this.save(userLocation);
        }

        // 4.返回更新后的用户位置信息
        // 要重新查询后再返回，才是最新的 -> 必须重新创建新的查询条件，不然两次用的一个查询条件
        QueryWrapper<UserLocation> newQueryWrapper = new QueryWrapper<>();
        newQueryWrapper.eq("userId", userLocation.getUserId());
        return this.getOne(newQueryWrapper);

    }


    /**
     * 获取当前登录用户的位置信息
     * @param loginUserId 当前登录用户Id
     * @return 用户位置信息
     */
    @Override
    public UserLocation getCurrentUserLocation(Long loginUserId) {
        // 调用getLocationByUserId()方法查询
        return getLocationByUserId(loginUserId);
    }


    /**
     * 获取指定用户位置信息
     * @param userId 用户Id
     * @return 用户位置信息
     */
    @Override
    public UserLocation getTheUserLocation(Long userId) {
        // 调用getLocationByUserId()方法查询
        return getLocationByUserId(userId);
    }


    /**
     * 查询用户位置信息的通用方法,不在ServiceImpl中设置返回VO，一般是在Controller层设置返回VO
     * @param userId 指定用户Id
     * @return 用户位置信息
     */
    private UserLocation getLocationByUserId(Long userId) {
        // 1.参数校验
        if (userId == null) {
            log.info("用户id为空");
        }

        // 2.新建查询条件
        QueryWrapper<UserLocation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);

        // 3.执行查询条件
        UserLocation loginUserLocation = this.getOne(queryWrapper);
        if (loginUserLocation == null) {
            log.info("用户{}的位置信息不存在", userId);
            return null;
        }


        // 4.返回结果
        // 4.1 如果有记录，返回Entity的UserLocation即可
        return loginUserLocation;

        /*// 4.1 如果有记录，则返回脱敏后的用户位置信息VO
        if (loginUserLocation != null) {
            // 4.2 新建UserLocationVO对象
            UserLocationVO loginLocationVO = new UserLocationVO();
            BeanUtils.copyProperties(loginUserLocation, loginLocationVO);
        }

        // 4.3 返回脱敏后的VO,为什么不能返回呢？
        return loginLocationVO);*/
    }
}

