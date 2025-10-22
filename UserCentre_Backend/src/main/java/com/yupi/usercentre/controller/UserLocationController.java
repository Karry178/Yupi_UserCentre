package com.yupi.usercentre.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yupi.usercentre.common.BaseResponse;
import com.yupi.usercentre.common.ErrorCode;
import com.yupi.usercentre.common.ResultUtils;
import com.yupi.usercentre.exception.BusinessException;
import com.yupi.usercentre.model.domain.User;
import com.yupi.usercentre.model.domain.UserLocation;
import com.yupi.usercentre.model.domain.UserTeam;
import com.yupi.usercentre.model.request.LocationUpdateRequest;
import com.yupi.usercentre.model.vo.TeamMemberDistanceVO;
import com.yupi.usercentre.model.vo.UserLocationVO;
import com.yupi.usercentre.service.TeamService;
import com.yupi.usercentre.service.UserLocationService;
import com.yupi.usercentre.service.UserService;
import com.yupi.usercentre.service.UserTeamService;
import com.yupi.usercentre.utils.GeoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/location")
// @CrossOrigin 跨域,默认所有的域名都允许跨域访问,可以使用origins={}指定域名
@CrossOrigin(
        origins = {"http://localhost:3000", "http://192.168.0.103:81", "http://192.168.0.103"},
        allowCredentials = "true"
)
@Slf4j
public class UserLocationController {

    // 注入userService层
    @Resource
    private UserService userService;

    // 注入UserTeamService层
    @Resource
    private UserTeamService userTeamService;

    // 注入UserLocationService层
    @Resource
    private UserLocationService userLocationService;

    // 注入TeamService层
    @Resource
    private TeamService teamService;

// todo 【已有的】匹配模式或者心动模式中已经做好了一个根据用户标签进行匹配的功能了，具体功能是：根据标签的相似度，在心动模式下可以优先展示几个与自己标签最为相近的用户。
// todo 【准备做的】现在可以做一个展示多个队伍中成员与自己最为相近的几个队伍，供登录用户选择加入哪个，这样的目的是保证用户能找到队伍中与自己相对最为接近的成员，模糊查询即可，优先保证在同一个城市，然后才是省份。而且我是否需要创建一个新的数据库呢？
    /**
     * todo 这个距离自己近的队伍，我想的是按照队伍里面离自己最近的人数百分比排序,假如有两个队伍，A队伍有4人，B队伍有3人
     * 帮我设计一下，如果我在成都，A队伍中我想的是有2个在成都，一个在深圳，一个在北京；B队伍中一个字成都，一个在重庆，一个在上海
     * 这样按照距离来计算，哪个队伍中总的离我最近的百分比更低一些？我优先选择这个队伍加入
      */



    /**
     * 【重难点】查询队伍成员位置(按距离排序)
     * @param teamId 是路径参数teamId
     * @param maxDistance 为什么加@RequestParam?
     * @param request 请求对象(获取登录用户)
     * @return 队伍成员位置列表
     *
     * 业务场景：
     * 1.用户打开队伍详情页，可以看到队友在哪
     * 2.可以显示每个队友与自己所处城市的距离
     * 3.按距离从近到远排序
     *
     * 权限要求：
     * - 必须登录
     * - 必须是该队伍的成员
     *
     * 异常情况：
     * - 用户未登录 → 401
     * - 用户不在队伍中 → 403
     * - 用户未设置位置 → 400
     * - 队伍不存在 → 404
     */
    @GetMapping("/team/{teamId}/members")
    public BaseResponse<List<TeamMemberDistanceVO>> getTeamMembersLocation(@PathVariable("teamId") Long teamId,
                                                                           @RequestParam(value = "maxDistance", required = false) Double maxDistance,
                                                                           HttpServletRequest request) {
        // 1.参数校验
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        if (maxDistance == null || maxDistance < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        // 2.获取登录用户
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_LOGIN, "用户未登录");
        }
          // 提前提取用户id
        Long currentUserId = loginUser.getId();

        // 3.【查询】权限验证 -> 验证用户是否字该队伍中
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId", teamId);
        queryWrapper.eq("userId", currentUserId);
        long count = userTeamService.count(queryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.NO_AUTH, "您不在该队伍中，无权限查看");
        }

        // 4.查询当前登录用户是否设置了位置
          // 4.1 查询当前用户位置信息
        UserLocation currentUserLocation = userLocationService.getById(currentUserId);
        if (currentUserLocation == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户未设置位置");
        }
          // 4.2 验证坐标有效性
        if (currentUserLocation.getLongitude() == null || currentUserLocation.getLatitude() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "您的位置信息不完整，请重新设置");
        }

        // 【核心内容】5.否则，调用Service获取该队伍中成员位置，并设置为列表，按照距离排序
        List<TeamMemberDistanceVO> memberLocationList = userLocationService.getTeamMembersLocation(teamId, maxDistance, loginUser.getId());

        // 【难点】6.过滤距离(如果提供了 maxDistance)
          // 6.1 判断是否需要过滤
        if (maxDistance != null) {
            // 6.2 使用Stream流过滤 -> 筛选距离小于maxDistance的成员
            memberLocationList = memberLocationList.stream()
                    .filter(member -> member.getDistance() <= maxDistance)
                    .collect(Collectors.toList());
        }

        // 7.返回结果 -> 包装成统一的响应格式
        return ResultUtils.success(memberLocationList);
    }


    /**
     * 更新用户位置
     * @param locationUpdateRequest 用户位置更新请求体
     * @param request 请求对象(获取登录用户)
     * @return 用户位置信息
     */
    @PutMapping("/user/location")
    public BaseResponse<UserLocationVO> updateUserLocation(@RequestBody @Valid LocationUpdateRequest locationUpdateRequest, HttpServletRequest request) {
        // 1.参数校验 -> locationUpdateRequest不能为空
        if (locationUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        // 2.获取登录用户
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_LOGIN, "用户未登录");
        }
        Long loginUserId = loginUser.getId();


        // 3.验证登录用户的经纬度有效性 -> 调用GeoUtils.isValidCoordinate()
          // 我在登录用户中没有加入坐标，我该怎么补进去呢？还是说调用其余的？
          // 理解错误：用户更新新的坐标是前端传来的，不是在后端获取

          // 3.1 先新建一个UserLocation对象 -> 存入登录用户的坐标
        UserLocation userLocation = new UserLocation();

          // 因为创建的是新的userLocation对象，所以里面是null，就不能赋值，因为赋值后还是null
        /*userLocation.setLongitude(userLocation.getLongitude());
        userLocation.setLatitude(userLocation.getLatitude());*/

          // 【改】3.2 应该从请求参数中获取坐标 -> request是前端用户输入后传到后端的请求体
        double longitude = locationUpdateRequest.getLongitude();
        Double latitude = locationUpdateRequest.getLatitude();

          // 再加入@Valid后，不需要再判断了
        /*if (!GeoUtils.isValidCoordinate(longitude, latitude)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "登录用户的经纬度无效");
        }*/

          // 3.3 赋值给创建好的userLocation对象
        userLocation.setUserId(loginUserId);
        userLocation.setCity(locationUpdateRequest.getCity());
        userLocation.setProvince(locationUpdateRequest.getProvince());
        userLocation.setLongitude(longitude); // 经纬度从请求参数中获取
        userLocation.setLatitude(latitude);


        // 4.调用userLocationService.updatedLocation()更新用户位置信息
        UserLocation updatedLocation = userLocationService.updatedLocation(userLocation);

        // 5.返回更新后的结果 -> 封装成UserLocationVO(脱敏)
        UserLocationVO userLocationVO = new UserLocationVO();
        userLocationVO.setUserId(loginUserId);
        userLocationVO.setCity(updatedLocation.getCity());
        userLocationVO.setProvince(updatedLocation.getProvince());
        userLocationVO.setUpdateTime(updatedLocation.getUpdateTime());

        return ResultUtils.success(userLocationVO);
    }


    /**
     * 查询当前登录用户位置
     * @param request 请求对象(获取登录用户)
     * @return 用户位置信息
     */
    @GetMapping("/user/location")
    public BaseResponse<UserLocationVO> getCurrentUserLocation(HttpServletRequest request) {

        // 1.获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_LOGIN, "用户未登录");
        }
        Long loginUserId = loginUser.getId();

        // 2.调用userLocationService.getCurrentUserLocation()查询登录用户位置
        UserLocation currentUserLocation = userLocationService.getCurrentUserLocation(loginUserId);
        if (currentUserLocation == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户未设置位置");
        }

        // 3.返回结果 -> 封装成UserLocationVO(脱敏)
        UserLocationVO userLocationVO = new UserLocationVO();
          // 【推荐】是否可以使用BeanUtils.propertiesCopy()进行赋值到对应的userLocationVO对象中？
          // -> 完全可以，且推荐这样！！！
          // 但是有一个前提条件 -> 源对象和目标对象属性名一致
        BeanUtils.copyProperties(currentUserLocation, userLocationVO);

        return ResultUtils.success(userLocationVO);
    }


    /**
     * 【重点】查询指定用户的位置
     * @param userId 指定用户id
     * @param request 请求对象(获取登录用户)
     * @return 指定用户位置信息
     *
     * 为什么要使用{userId}？-> 可以获取指定id的用户位置
     */
    @GetMapping("/user/{userId}")
    public BaseResponse<UserLocationVO> getUserLocation(@PathVariable("userId") Long userId, HttpServletRequest request) {
        // 1.参数校验 -> userId 不能为空
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        // 2.获取登录用户
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_LOGIN, "用户未登录");
        }
        Long loginUserId = loginUser.getId();

        // 3.权限验证
          // 3.1 如果查询自己，则允许查询
        if (userId.equals(loginUserId)) {
            return getCurrentUserLocation(request);
        }

          // 3.2 如果查询他人，需要验证是否为队友
        if (!userTeamService.isTeamMember(userId, loginUserId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "不是你的队友，你无权限查询");
        }


          // 【难点】3.3 检查对方的隐私设置 不懂！
        /**
         * 什么是隐私设置：
         *
         * 用户可以设置位置的可见性：
         * - 0：公开（所有人可见）
         * - 1：仅队友可见
         * - 2：不公开（只有自己可见）
         *
         * 在 user_location 表中：
         * location_privacy TINYINT DEFAULT 1
         *
         *
         * 场景：
         * 用户A和用户B是队友
         * 用户B设置了位置隐私为"不公开"
         *
         * 如果不检查隐私设置：
         * 用户A可以看到用户B的位置 ❌
         *
         * 如果检查隐私设置：
         * 用户A无法看到用户B的位置 ✅（尊重隐私）
         */

        // 3.3 查询指定用户的隐私设置 -> 调用userLocationService.getTheUserLocation()
        UserLocation theUserLocation = userLocationService.getTheUserLocation(userId);
        if (theUserLocation == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该用户未设置位置");
        }

        // 获取隐私设置
        Integer locationPrivacy = theUserLocation.getLocationPrivacy();

        // 判断隐私设置
        if (locationPrivacy == null) {
            locationPrivacy = 1; // 默认为仅队友可见
        }
        if (locationPrivacy == 2) {
            throw new BusinessException(ErrorCode.NO_AUTH, "该用户位置隐私设置不公开，你无访问权限");
        }

        // 4.返回结果 根据隐私设置返回不同粒度的信息 看不懂这句意思 -> 封装成UserLocationVO(脱敏)
          // 不同粒度的意思是：粒度1：完整信息；粒度2：模糊信息；粒度3：省略信息(如只显示省份)
          // 推荐直接按照粒度1进行返回
        UserLocationVO userLocationVO = new UserLocationVO();
        BeanUtils.copyProperties(theUserLocation, userLocationVO);

        return ResultUtils.success(userLocationVO);
    }


    /**
     * 计算两点距离(工具接口)
     * @param lon1 纬度1
     * @param lat1 经度1
     * @param lon2 纬度2
     * @param lat2 经度2
     * @return 距离，单位：千米
     */
    @GetMapping("/distance")
    public BaseResponse<String> calculateDistance(
            @RequestParam("lon1") Double lon1,
            @RequestParam("lat1") Double lat1,
            @RequestParam("lon2") Double lon2,
            @RequestParam("lat2") Double lat2) {

        // 1.参数校验 -> lon1, lat1, lon2, lat2 不能为空
        if (lon1 == null || lon2 == null || lat1 == null || lat2 == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        // 2.验证坐标有效性
        if (!GeoUtils.isValidCoordinate(lon1, lat1) || !GeoUtils.isValidCoordinate(lon2, lat2)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "经纬度无效");
        }

        // 3.调用GeoUtils.calculateDistance()计算两点距离
        double distance = GeoUtils.calculateDistance(lon1, lat1, lon2, lat2);

        // 4.返回格式化后的距离
        return ResultUtils.success(GeoUtils.formatDistance(distance));
    }
}
