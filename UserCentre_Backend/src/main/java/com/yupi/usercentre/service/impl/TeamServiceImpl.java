package com.yupi.usercentre.service.impl;
import java.util.Date;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.usercentre.common.ErrorCode;
import com.yupi.usercentre.exception.BusinessException;
import com.yupi.usercentre.model.domain.Team;
import com.yupi.usercentre.mapper.TeamMapper;
import com.yupi.usercentre.model.domain.User;
import com.yupi.usercentre.model.domain.UserLocation;
import com.yupi.usercentre.model.domain.UserTeam;
import com.yupi.usercentre.model.dto.TeamQuery;
import com.yupi.usercentre.model.dto.TeamScoreDTO;
import com.yupi.usercentre.model.enums.TeamStatusEnum;
import com.yupi.usercentre.model.request.TeamJoinRequest;
import com.yupi.usercentre.model.request.TeamQuitRequest;
import com.yupi.usercentre.model.request.TeamUpdateRequest;
import com.yupi.usercentre.model.vo.TeamRecommendVO;
import com.yupi.usercentre.model.vo.TeamUserVO;
import com.yupi.usercentre.model.vo.UserVO;
import com.yupi.usercentre.service.TeamService;
import com.yupi.usercentre.service.UserLocationService;
import com.yupi.usercentre.service.UserService;
import com.yupi.usercentre.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
* @author 17832
* @description 针对表【team(队伍)】的数据库操作Service实现
* @createDate 2025-10-05 14:45:57
*/
@Service
@Slf4j
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService{

    // 引入用户-队伍关系表
    @Resource
    private UserTeamService userTeamService;

    // 引入用户表
    @Resource
    private UserService userService;

    // 引入Redisson -> 目的是引入分布式锁
    @Resource
    private RedissonClient redissonClient;

    // 引入UserLocationService
    @Resource
    private UserLocationService userLocationService;


    /**
     * 添加/创建队伍
     * @param team 队伍信息
     * @param loginUser 当前登录用户
     * @return
     *
     * 1.请求参数是否为空？
     * 2.是否登录，未登录不允许创建
     * 3.校验信息
     *    1.队伍人数>1且<= 20
     *    2.队伍标题<= 20
     *    3. 描述<= 512
     *    4.status是否公开（int）不传默认为0（公开）
     *    5.如果status是加密状态，一定要有密码，且密码<=32
     *    6.超时时间>当前时间
     *    7.校验用户最多创建5个队伍
     *4.插入队伍信息到队伍表
     *5.插入用户=>队伍关系到关系表
     */
    @Override
    @Transactional(rollbackFor = Exception.class) // 开启事务，并捕获全部异常
    public long addTeam(Team team, User loginUser) {
        // 1.请求参数是否为空？
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        // 2.是否登录，未登录不允许创建
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_LOGIN);
        }
          // 再提取当前登录用户的Id -> 加final表示不能被修改
        final Long userId = loginUser.getId();

        // ========== 使用分布式锁解决并发问题 ==========
        // 构建锁的key
        String lockKey = String.format("user:create:team:%s", userId);

        // 2.获取锁对象
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 3.尝试获取锁
            boolean isLock = lock.tryLock(0, -1, TimeUnit.SECONDS);

            if (!isLock) {
                // 获取锁失败，说明用户正在创建队伍
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙");
            }
            // 4.否则，说明获取锁成功，执行创建队伍的方法


            // ============== 下面这部分是 原创建队伍的检验方法 3.校验信息 ===================
            // 1.队伍人数>1且<= 20;  Optional.ofNullable(team.getMaxNum()).orElse(0)：获取maxNum的值，如果maxNum为null，则返回0
            int maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
            if (maxNum < 1 || maxNum > 20) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,"组队人数不符合要求");
            }
            // 2.队伍标题长度<= 20，但不能为空
            String name = team.getName();
            if (StringUtils.isBlank(name) || name.length() > 20){
                throw new BusinessException(ErrorCode.PARAM_ERROR,"队伍标题不符合要求");
            }
            // 3.队伍描述 <= 512
            String description = team.getDescription();
            if (StringUtils.isBlank(description) && description.length() > 512){
                throw new BusinessException(ErrorCode.PARAM_ERROR,"队伍描述不符合要求");
            }
            // 4.status是否公开（int）不传默认为0（公开）
            int status = Optional.ofNullable(team.getStatus()).orElse(0);
            // 获取队伍状态枚举值
            TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
            if (statusEnum == null){
                throw new BusinessException(ErrorCode.PARAM_ERROR,"队伍状态不满足要求");
            }
            // 5.如果status是加密状态，一定要有密码，且密码<=32
            String password = team.getPassword();
            if (TeamStatusEnum.SECRET.equals(statusEnum) && (StringUtils.isBlank(password)) || password.length() > 32){
                throw new BusinessException(ErrorCode.PARAM_ERROR,"队伍密码设置不正确");
            }
            // 6.超时时间>当前时间
            Date expireTime = team.getExpireTime();
            // expireTime是超时时间，如果当前时间在超时时间之后，则返回错误
            if (new Date().after(expireTime)){
                throw new BusinessException(ErrorCode.PARAM_ERROR,"超时设置不符合要求");
            }
            // 7.校验用户最多创建5个队伍
            // todo BUG是如果用户创建队伍时疯狂点击，可能同时创建几十个队伍！
            QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userId",userId);
            long hasTeamNum = this.count(queryWrapper); // 获取当前用户创建的队伍数量
            if (hasTeamNum >= 5){
                throw new BusinessException(ErrorCode.PARAM_ERROR,"最多创建5个队伍");
            }

            // 4.插入队伍信息到队伍表
            team.setId(null); // 插入之前，将Id设置为null，让MyBatis-Plus自动生成Id
            team.setUserId(userId);
            boolean result = this.save(team);
            Long teamId = team.getId(); // 获取刚创建的队伍的Id

            if (!result || teamId == null){
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"创建队伍失败");
            }

            // 5.插入用户=>队伍关系到关系表
            UserTeam userTeam = new UserTeam();
            userTeam.setUserId(userId);
            userTeam.setTeamId(teamId);
            userTeam.setJoinTime(new Date());
            result = userTeamService.save(userTeam);
            if (!result){
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"创建队伍失败");
            }

            return teamId;

        } catch (InterruptedException e) {
            log.error("获取锁失败", e);
            // 获取锁失败，返回错误
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙");
        } finally {
            // 只能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                lock.forceUnlock();
            }
        }

    }



    /**
     * 搜索队伍列表
     *
     * @param teamQuery 队伍查询参数
     * @param isAdmin 是否是管理员
     * @return
     */
    @Override
    public List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin) {
        // 1.新建查询
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        if (teamQuery != null) {
            Long id = teamQuery.getId();
            if (id != null && id > 0){
                queryWrapper.eq("id",id); // 获取队伍Id
            }

            // 新加：获取用户加入的队伍列表
            List<Long> idList = teamQuery.getIdList();
            if (idList != null && !idList.isEmpty()) {
                queryWrapper.in("id",idList);
            }

            String searchText = teamQuery.getSearchText();
            if (StringUtils.isNotBlank(searchText)) {
                // 使用and连接两个模糊查询：可以通过name，也可以通过description 模糊查询
                queryWrapper.and(qw -> qw.like("name",searchText).or().like("description",searchText));
            }

            String name = teamQuery.getName();
            if (StringUtils.isNotBlank(name)) {
                queryWrapper.like("name",name); // 使用like 模糊查询，获取队伍名称
            }

            String description = teamQuery.getDescription();
            if (StringUtils.isNotBlank(description)) {
                queryWrapper.like("description",description);
            }

            Integer maxNum = teamQuery.getMaxNum();
            if (maxNum != null && maxNum > 0){
                queryWrapper.eq("maxNum",maxNum);
            }

            // 根据创建人来查询
            Long userId = teamQuery.getUserId();
            if (userId != null && userId > 0){
                queryWrapper.eq("userId",userId);
            }

            // 根据队伍状态查询
            Integer status = teamQuery.getStatus();
            TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
            
            // 如果指定了状态
            if (statusEnum != null) {
                // 如果当前登录用户不是管理员且状态不是公开，则不能查询
                if (!isAdmin && !statusEnum.equals(TeamStatusEnum.PRIVATE)){
                    throw new BusinessException(ErrorCode.NO_AUTH);
                }
                // 添加状态过滤条件
                queryWrapper.eq("status", statusEnum.getValue());
            } else if (!isAdmin) {
                // 如果没有指定状态，且当前用户不是管理员，只能查询公开的队伍
                queryWrapper.eq("status", TeamStatusEnum.PUBLIC.getValue());
            }
            // 如果是管理员且没有指定状态，则不添加status过滤条件，可以查询所有状态的队伍
        }

        // 不能展示已经过期的队伍  expireTime is null or expireTime > now()
        queryWrapper.and(qw -> qw.gt("expireTime",new Date()).or().isNull("expireTime"));

        List<Team> teamList = this.list(queryWrapper);
        if (CollectionUtils.isEmpty(teamList)){
            return new ArrayList<>();
        }
        // 2.关联查询创建人的用户信息
        /*
        一、自己写SQL
        查询队伍和创建人的信息
        select * from team t left join user u on t.userId = u.id
        查询队伍和已加入的成员信息
        select *
            from team t
                left join user_team ut on t.id = ut.teamId
                left join user u on ut.userId = u.id;
         */
        List<TeamUserVO> teamUserVOList = new ArrayList<>();
        // 遍历获取创建人的用户信息
        for (Team team : teamList){
            Long userId = team.getUserId();
            if (userId == null){
                continue;
            }
            // 通过userService层根据用户Id获取用户信息
            User user = userService.getById(userId);
            // 脱敏用户信息
            User safetyUser = userService.getSafetyUser(user);

            // 3.映射 teamUserVO --> teamUserVO,即从team映射同步到teamUserVO
            TeamUserVO teamUserVO = new TeamUserVO();
            // 使用Spring的BeanUtils，参数顺序是源对象，目标对象
            BeanUtils.copyProperties(team, teamUserVO);

            // ========== 在这里添加查询已加入人数的代码 ==========
            QueryWrapper<UserTeam> joinNumQueryWrapper = new QueryWrapper<>();
            joinNumQueryWrapper.eq("teamId", team.getId()); // teamId必须要在已查询到的队伍列表中
            joinNumQueryWrapper.eq("isDelete", 0); // 条件：仅未删除的才可以被查到
            long hasJoinNum = userTeamService.count(joinNumQueryWrapper);
              // 设置已加入人数到teamUserVO对象中
            teamUserVO.setHasJoinNum((int) hasJoinNum);


            // 再创建一个userVO,返回脱敏后的用户信息
            if (user != null) {
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(user,userVO);
                teamUserVO.setCreateUser(userVO); // 把查到的用户信息赋给teamUserVO，返回给前端脱敏后的数据
            }
            teamUserVOList.add(teamUserVO);
        }
        
        return teamUserVOList;
    }


    /**
     * 更新队伍信息
     * @param teamUpdateRequest 队伍信息
     * @param loginUser 当前登录用户
     * @return 更新结果
     */
    @Override
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser) {
        // 1.先判断是否为空
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        // 2.再对id进行判断
        Long id = teamUpdateRequest.getId();
        if (id == null || id <= 0){
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        // 3.再根据id查询队伍 -> 如果老队伍为空，表示无队伍，直接抛异常
        Team oldTeam = this.getById(id);
        if (oldTeam == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR,"请求队伍不存在");
        }
        // 4.判断当前登录用户是否是队伍创建者或者管理员
        if (!oldTeam.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        // 5.判断：如果队伍状态改为加密，必须要有密码
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(teamUpdateRequest.getStatus());
        if (TeamStatusEnum.SECRET.equals(statusEnum)) {
            // 5.1 没有传递密码
            if (StringUtils.isBlank(teamUpdateRequest.getPassword())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,"加密队伍的话，必须要有密码");
            }
        }

        // 6.创建一个新的队伍updateTeam,映射teamUpdateRequest -> teamUpdateTeam
        Team updateTeam = new Team();
        BeanUtils.copyProperties(teamUpdateRequest, updateTeam);
          // 然后更新
        boolean result = this.updateById(updateTeam);
        return result;
    }


    /**
     * 加入队伍
     * @param teamJoinRequest 要加入的队伍信息
     * @param loginUser 当前登录用户
     * @return
     */
    @Override
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {
        // 1.边界检查
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        // 2.查询
          // 2.1部分 -> 查数据库的内容尽量放在后面，因为数据库查询性能一般，放在后面了

          // 2.2 查询当前队伍id是否合法
        Long teamId = teamJoinRequest.getTeamId();
          // 2.3 获取当前队伍信息
        Team team = getTeamById(teamId);// 队伍id合法，就根据其id查询队伍信息
          // 2.4 用户只能加入未过期的队伍
        Date expireTime = team.getExpireTime();
        if (team.getExpireTime() != null && team.getExpireTime().before(new Date())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,"队伍已过期");
        }
          // 2.5 获取当前队伍的状态 -> 禁止加入私有的队伍 -> 或者有密码的话，密码正确才可以加入需要密码的队伍
        Integer status = team.getStatus();
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(status);// 获取当前队伍的状态的枚举值
        if (TeamStatusEnum.PRIVATE.equals(teamStatusEnum)) { // 要保证非空的放在equal前面
            throw new BusinessException(ErrorCode.PARAM_ERROR,"禁止加入私有队伍");
        }
        String password = teamJoinRequest.getPassword();
        if (TeamStatusEnum.SECRET.equals(teamStatusEnum)) {
            // 密码为空或错误，抛异常
            if (StringUtils.isBlank(password) || !password.equals(team.getPassword())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,"密码错误");
            }
        }

          // 下面为通过数据库查询的内容
          // 加分布式锁(锁要加在对象上) -> 目的：锁进来时，可以让用户一个个的执行锁住的这段方法，避免用户加入多个队伍，导致数据错误
            // 2.1 查询当前用户加入队伍的数量(自己创建的也算加入)
            Long userId = loginUser.getId();
        // 创建一个分布式锁 - redisson -> 只有一个线程能够获取到锁
        RLock lock = redissonClient.getLock("yupao:join_team");
        try {
            while (true) {
                if (lock.tryLock(0,-1, TimeUnit.MILLISECONDS)) {
                    // 获取锁成功，打印其线程id
                    System.out.println("getLock:" + Thread.currentThread().getId());

                    QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                    userTeamQueryWrapper.eq("userId", userId);
                    long hasJoinNum = userTeamService.count(userTeamQueryWrapper);
                    if (hasJoinNum >= 5) {
                        throw new BusinessException(ErrorCode.PARAM_ERROR, "最多创建和加入5个队伍");
                    }

                    // 2.6 不能重复加入已加入的队伍
                    userTeamQueryWrapper = new QueryWrapper<>();
                    userTeamQueryWrapper.eq("userId", userId);
                    userTeamQueryWrapper.eq("teamId", teamId);
                    long hasUserJoinTeam = userTeamService.count(userTeamQueryWrapper);
                    if (hasUserJoinTeam > 0) {
                        throw new BusinessException(ErrorCode.PARAM_ERROR, "用户已加入该队伍");
                    }

                    // 2.7 只能加入未满人的队伍
                    long teamHasJoinNum = countTeamUserByTeamId(teamId); // 调用获取队伍人数方法
                    if (teamHasJoinNum >= team.getMaxNum()) {
                        throw new BusinessException(ErrorCode.PARAM_ERROR,"队伍已满");
                    }

                    // 最后修改队伍信息 -> 新增队伍-用户关联信息
                    UserTeam userTeam = new UserTeam(); // 新增用户队伍
                    userTeam.setUserId(userId); // 把新加入的用户Id和队伍Id以及用户加入队伍的时间存起来
                    userTeam.setTeamId(teamId);
                    userTeam.setJoinTime(new Date());
                    return userTeamService.save(userTeam); // 把数据通过service层保存到数据库中
                }
            }

        } catch (InterruptedException e) {
            log.error("doCacheRecommendUser error", e);
            return false;
        } finally {
            // 释放锁部分
            if (lock.isHeldByCurrentThread()) {
                // 判断：只有当前线程持有锁，才释放锁
                System.out.println("unLock：" + Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }


    /**
     * 用户退出队伍
     * @param teamQuitRequest
     * @param loginUser
     * @return
     */
    @Override
    public boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser) {
        // 1. 边界检查
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        /*// 2. 获取队伍id，判断队伍id是否合法
        Long teamId = teamQuitRequest.getTeamId();
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        // 3.拿到队伍Id后判断队伍是否存在
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR,"队伍不存在");
        }*/
        Long teamId = teamQuitRequest.getTeamId();
        Team team = getTeamById(teamId);
        // 4.获取当前登录用户id，判断其是否已经加入当前队伍 -> 建立用户队伍关联表
        Long userId = loginUser.getId();
        UserTeam queryUserTeam = new UserTeam(); // 创建用户队伍关联表
        queryUserTeam.setUserId(userId); // 将当前用户Id和队伍Id存起来
        queryUserTeam.setTeamId(teamId);
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>(queryUserTeam); // 把查询条件封装成queryWrapper
        long count = userTeamService.count(queryWrapper); // 通过service层查询用户是否已经加入当前队伍 --> 0为未加入
        if (count == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,"未加入该队伍");
        }
        // 5.获取队伍的成员数量
        long teamHasJoinNum = this.countTeamUserByTeamId(teamId);
        if (teamHasJoinNum == 1) {
            // 队伍人数为1，解散/删除该队伍和所有加入队伍的关系
            this.deleteTeam(teamId, loginUser);
            QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
            userTeamQueryWrapper.eq("teamId",teamId);
            return userTeamService.remove(userTeamQueryWrapper);
        } else {
            // 5.1 队伍还有其他人，判断是否为队长 -> 即是否为队伍创建人
            if (team.getUserId().equals(userId)) {
                // 把队伍转移给最早加入的用户
                  // 先查询已加入队伍的所有用户和加入时间
                QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                userTeamQueryWrapper.eq("teamId",teamId);
                  // 获取查询到的队伍中的最前面的2个id对应数据
                userTeamQueryWrapper.last("order by id asc limit 2");
                List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
                if (CollectionUtils.isEmpty(userTeamList) || userTeamList.size() <= 1) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR);
                }
                  // 获取下一个用户的信息
                UserTeam nextUserTeam = userTeamList.get(1);
                Long nextTeamLeaderId = nextUserTeam.getUserId(); // 获取下一个用户的id
                  // 更新当前队伍的新队长
                Team updateTeam = new Team();
                updateTeam.setId(teamId);
                updateTeam.setUserId(nextTeamLeaderId);
                boolean result = this.updateById(updateTeam);
                if (!result) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR,"更新队伍的队长失败");
                }
                  // 如果更新队长成功 -> 移除当前用户的队伍关系
                return userTeamService.remove(queryWrapper);
            } else {
                // 5.2 用户不是队长，直接退出即可
                return userTeamService.remove(queryWrapper);
            }
        }
    }


    /**
     * 删除队伍
     * @param id
     * @param loginUser
     * @return
     */
    @Override
    @Transactional (rollbackFor = Exception.class)
    public boolean deleteTeam(long id, User loginUser) {
        // 1.检验队伍是否存在
        Team team = getTeamById(id);
        // 2.检验登录用户是否为队伍的队长
        if (!team.getUserId().equals(loginUser.getId())){
            throw new BusinessException(ErrorCode.NO_AUTH,"无访问权限");
        }
        // 3.移除所有加入队伍的关联信息 -> 先根据队伍id查询所有加入该队伍的用户，再删除
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        Long teamId = team.getId(); // 按住Ctrl + Alt + V 可以快速生成变量，并替换全部匹配变量
        userTeamQueryWrapper.eq("teamId", teamId);
        boolean result = userTeamService.remove(userTeamQueryWrapper);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除队伍关联信息失败");
        }
        // 4.删除队伍
        this.removeById(teamId);
        return false;
    }


    /**
     * 获取当前队伍的成员数量
     * @param teamId
     * @return
     */
    private long countTeamUserByTeamId(long teamId){
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId",teamId);
        return userTeamService.count(userTeamQueryWrapper);
    }


    /**
     * 根据ID获取当前队伍信息
     * @param teamId
     * @return 队伍信息
     */
    private Team getTeamById(Long teamId) {
        // 2. 获取队伍id，判断队伍id是否合法
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        // 3.拿到队伍Id后判断队伍是否存在
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR,"队伍不存在");
        }
        return team;
    }


    // ================================= 【重难点】新功能：队伍推荐(心动)模式方法实现类 =================================

    /**
     * 获取推荐的队伍列表
     * @param userId 当前登录用户Id
     * @param pageSize 每页显示队伍数量
     * @return 推荐的队伍列表
     *
     * 实现步骤：
     * 1. 获取用户信息和位置
     * 2. 查询所有可加入的队伍
     * 3. 过滤已加入的队伍
     * 4. 计算每个队伍的得分
     * 5. 按得分排序
     * 6. 取Top N
     * 7. 转换成VO返回
     */
    @Override
    public List<TeamRecommendVO> getRecommendTeams(Long userId, Integer pageSize) {

        // ========== 步骤1：获取用户信息 ==========
        // 1.1 获取登录用户信息
        User loginUser = userService.getById(userId);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_LOGIN, "当前未登录用户");
        }

        // 1.2 获取登录用户的位置信息
        UserLocation currentUserLocation = userLocationService.getCurrentUserLocation(loginUser.getId());
        if (currentUserLocation == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "当前用户位置信息不存在");
        }

        // 1.3 提取用户城市和省份
        String userCity = currentUserLocation.getCity();
        String userProvince = currentUserLocation.getProvince();


        // ========== 步骤2：查询所有可加入的队伍 ==========
        // 2.1 构建查询条件：可加入的队伍，且未加入的队伍
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 0) // 只查公开队伍
                    .gt("expireTime", new Date()); // 未过期

        // 2.2 查询所有可加入的队伍，加入列表
        List<Team> allTeams = this.list(queryWrapper);

        // 2.3 判断查到的队伍数量是否为0
        if (allTeams.isEmpty()) {
            return Collections.emptyList();
        }

        // ========== 步骤3：过滤已加入的队伍 ==========
        // 3.1 查询用户已加入的队伍Id列表
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId", userId);
        userTeamQueryWrapper.select("teamId"); // 只查询队伍Id这列
        List<UserTeam> joinedTeams = userTeamService.list(userTeamQueryWrapper); // 拿到用户已经加入的队伍Id

        // 3.2 使用stream().map() 提取已加入的队伍Id
        List<Long> joinedTeamIds = joinedTeams.stream()
                .map(UserTeam::getTeamId)
                .collect(Collectors.toList());

        // 3.3 过滤已加入的队伍 -> 使用stream().filter()
        List<Team> candidateTeams = allTeams.stream()
                .filter(team -> !joinedTeamIds.contains(team.getId()))
                .collect(Collectors.toList());

        // 3.4 判断是否有候选队伍
        // todo 这个功能是什么意思呢？解释一下逻辑，我没看懂代码
        if (candidateTeams.isEmpty()) {
            log.info("用户{}已加入所有队伍", userId);
            return Collections.emptyList();
        }

        // ========== 【核心】步骤4：计算每个队伍的总得分 ==========
        // 4.1 创建得分列表
        ArrayList<TeamScoreDTO> teamScores = new ArrayList<>();

        // 4.2 遍历每个候选队伍
        for (Team team : candidateTeams) {
            // 4.2.1 查询队伍成员Id列表
            QueryWrapper<UserTeam> memberQueryWrapper = new QueryWrapper<>();
            memberQueryWrapper.eq("teamId", team.getId()) // 查询队伍Id
                    .select("userId") // 只查询用户Id列
                    .groupBy("userId"); // 按照队伍成员Id进行分组
            List<UserTeam> teamMembers = userTeamService.list(memberQueryWrapper);

            // 4.2.2 提取成员Id列表
            List<Long> memberIds = teamMembers.stream()
                    .map(UserTeam::getUserId)
                    .collect(Collectors.toList());

            // 4.2.3 如果队伍中没有成员，跳过
            if (memberIds.isEmpty()) {
                continue;
            }

            // 4.2.4 批量查询成员位置信息
            QueryWrapper<UserLocation> locationQueryWrapper = new QueryWrapper<>();
            locationQueryWrapper.in("userId", memberIds)
                    .select("userId")
                    .select("city","province","longitude","latitude");
              // 对上面查询套件查到的成员位置信息列为一个List
            List<UserLocation> memberLocations = userLocationService.list(locationQueryWrapper);


            // ========== 4.3 计算同城成员比例 ==========
            // 4.3.1 统计(过滤出)同城成员数量 -> 使用stream().filter()
            // 下面这两句统计同城与同省成员的代码错误原因是什么？
            // filter()方法中的条件表达式需要返回boolean值
            long sameCityCount = memberLocations.stream()
                    // 判断登录用户的城市是否等于当前成员的城市
                    .filter(userLocation ->userCity.equals(userLocation.getCity()))
                    .count();

            // 4.3.2 计算同城成员比例
            double sameCityRatio = (double) sameCityCount / memberIds.size();


            // ========== 4.4 计算同省成员比例 ==========
            // 4.4.1 统计同城成员数量
            long sameProvinceCount = memberLocations.stream()
                    .filter(userLocation -> userProvince.equals(userLocation.getProvince()))
                    .count();

            // 4.4.2 计算同省成员比例
            double sameProvinceRatio = (double) sameProvinceCount / memberIds.size();


            // ========== 【重要】4.5 计算用户与整个队伍的标签相似度 ==========
            // 4.5.1 批量查询成员信息(获取标签)
            List<User> members = userService.listByIds(memberIds);

            // 4.5.2 计算标签平均相似度 -> 使用userService.calculateTagSimilarity()工具类方法
            double tagSimilarity = userService.calculateAvgTagSimilarity(loginUser, members);
            log.info("队伍{}的标签相似度为：{}", team.getId(), tagSimilarity);

            // ========== 4.6 计算综合得分 ==========
            // 4.6.1 给每个属性定义权重
            double CITY_WEIGHT = 0.3; // 同城权重：30%
            double TAG_WEIGHT = 0.5; // 标签权重：50%
            double PROVINCE_WEIGHT = 0.2; // 同省权重：20%

            // 4.6.2 计算队伍的综合得分
            double score = sameCityRatio * CITY_WEIGHT + tagSimilarity * TAG_WEIGHT + sameProvinceRatio * PROVINCE_WEIGHT;

            // ========== 4.7 计算城市分布（可选）==========
            // 4.7.1 统计每个城市的成员数量 -> 使用stream().filter()
            /**
             * 过滤出城市信息非空的成员，并使用stream().collect(Collectors.groupingBy())排序
             * 按照城市分组，并使用Collectors.collectingAndThen() -> 意思是先统计，再做别的内容 -> 统计后，Long::intValue 是 转换为int类型
             */
            Map<String, Integer> cityDistribution = memberLocations.stream()
                    .filter(UserLocation -> UserLocation.getCity() != null)
                    .collect(Collectors.groupingBy(
                            UserLocation::getCity,
                            Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));


            // ========== 4.8 构建得分DTO ==========
            // todo 我没明白这个DTO具体的作用是什么？下面这部分代码是什么意思？作用是什么？
            TeamScoreDTO scoreDTO = new TeamScoreDTO();
            scoreDTO.setTeam(team);
            scoreDTO.setScore(score);
            scoreDTO.setSameCityRatio(sameCityRatio);
            scoreDTO.setSameProvinceRatio(sameProvinceRatio);
            scoreDTO.setTagSimilarity(tagSimilarity);
            scoreDTO.setCityDistribution(cityDistribution);

            teamScores.add(scoreDTO);

        }

        // ========== 步骤5：按得分排序 ==========
        /**
         * 5.1 按照得分降序排序
         * comparing: 比较器，指定排序字段
         * reversed: 降序
          */

        teamScores.sort(Comparator.comparing(TeamScoreDTO::getScore).reversed());

        // ========== 步骤6：取Top N ==========
        // 6.1 限制返回的数量，只允许取前N个结果展示出来 -> stream().limit()
        List<TeamScoreDTO> topTeams = teamScores.stream()
                .limit(pageSize)
                .collect(Collectors.toList());

        // ========== 步骤7：赋值给VO返回 ==========

        // 7.1 转换为TeamRecommendVO列表
        List<TeamRecommendVO> teamRecommend = topTeams.stream()
                .map(teamScoreDTO -> convertToRecommendVO(teamScoreDTO, userCity))
                .collect(Collectors.toList());

        return teamRecommend;
    }



    /**
     * 封装TeamRecommendVO
     * @param teamScoreDTO 队伍得分DTO
     * @param userCity 用户所在城市
     * @return TeamRecommendVO
     */
    private TeamRecommendVO convertToRecommendVO(TeamScoreDTO teamScoreDTO, String userCity) {

        // 从teamScoreDTO中获取队伍信息
        Team team = teamScoreDTO.getTeam();

        // 1.创建VO对象
        TeamRecommendVO teamRecommendVO = new TeamRecommendVO();

        // 2.复制队伍基本信息
        BeanUtils.copyProperties(team, teamRecommendVO);

        // 3.查询当前人数
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId", team.getId()); // 筛选出队伍
          // 调用Service层方法查询当前人数
        long currentNum = userTeamService.count(queryWrapper);
          // 把当前队伍人数赋给VO对象
        teamRecommendVO.setCurrentNum((int) currentNum);

        // 4.设置推荐得分信息 -> 也就是在封装VO对象中，添加了很多关于得分的属性
        teamRecommendVO.setScore(teamScoreDTO.getScore());
        teamRecommendVO.setSameCityRatio(teamScoreDTO.getSameCityRatio());
        teamRecommendVO.setSameProvinceRatio(teamScoreDTO.getSameProvinceRatio());
        teamRecommendVO.setTagSimilarity(teamScoreDTO.getTagSimilarity());
        teamRecommendVO.setCityDistribution(teamScoreDTO.getCityDistribution());

        return teamRecommendVO;
    }
}




