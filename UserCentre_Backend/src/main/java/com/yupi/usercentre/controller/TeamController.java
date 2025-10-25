package com.yupi.usercentre.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.usercentre.common.BaseResponse;
import com.yupi.usercentre.common.DeleteRequest;
import com.yupi.usercentre.common.ErrorCode;
import com.yupi.usercentre.common.ResultUtils;
import com.yupi.usercentre.model.domain.User;
import com.yupi.usercentre.model.domain.UserLocation;
import com.yupi.usercentre.model.domain.UserTeam;
import com.yupi.usercentre.model.dto.ChatQuery;
import com.yupi.usercentre.model.dto.TeamQuery;
import com.yupi.usercentre.exception.BusinessException;
import com.yupi.usercentre.model.domain.Team;
import com.yupi.usercentre.model.request.*;
import com.yupi.usercentre.model.vo.ChatVO;
import com.yupi.usercentre.model.vo.TeamRecommendVO;
import com.yupi.usercentre.model.vo.TeamUserVO;
import com.yupi.usercentre.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;


/**
 * 队伍接口
 */
@RestController
@RequestMapping("/team")
// @CrossOrigin 跨域,默认所有的域名都允许跨域访问,可以使用origins={}指定域名
@CrossOrigin(
        origins = {"http://localhost:3000", "http://192.168.0.103:81", "http://192.168.0.103"},
        allowCredentials = "true"
)
@Slf4j
public class TeamController {

    // controller层要调用Service层,引入Service层
    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    @Resource
    private UserTeamService UserTeamService;

    // 引入RedisTemplate
    /*@Resource
    private RedisTemplate<String,Object> redisTemplate;*/

    // 引入ChatMessageService
    @Resource
    private ChatMessageService chatMessageService;

    // 引入UserLocationService
    @Resource
    private UserLocationService userLocationService;


    /**
     * 创建新的队伍
     * @param teamAddRequest 创建队伍的参数
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request){
        // 边界检测
        if (teamAddRequest == null){
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
          // 映射 teamAddRequest --> team
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest,team);
        // 创建队伍 -> 从Service层中调用addTeam的方法
        long teamId = teamService.addTeam(team,loginUser);

        // 创建成功后，系统会自动为其生成一个Id，回写到team对象中
        return ResultUtils.success(teamId);
    }


    /**
     * 通过队伍Id删除队伍
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request){
        // 边界检查
        if (deleteRequest == null || deleteRequest.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        // 删除队伍操作
        long id = deleteRequest.getId();
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.deleteTeam(id, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除失败");
        }
        return ResultUtils.success(true);
    }


    /**
     * 修改队伍参数
     * @param teamUpdateRequest 更新队伍的参数
     * @param request
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request){
        // 边界检查
        if (teamUpdateRequest == null){
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 修改队伍操作
        boolean result = teamService.updateTeam(teamUpdateRequest, loginUser);
        if (!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"修改失败");
        }
        return ResultUtils.success(true);
    }


    // 查：通过队伍Id查询
    @GetMapping("/get")
    public BaseResponse<Team> getTeamById(@RequestParam long id){
        // 边界检查
        if (id <= 0){
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        // 根据Id查询队伍
        Team team = teamService.getById(id);
        // 判断队伍是否存在
        if (team == null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return ResultUtils.success(team);
    }


    /**
     * 搜索队伍列表,并且只有登录后才可以看
     * @param teamQuery 队伍查询参数
     * @param request
     * @return
     */
    @GetMapping("/list")
    public BaseResponse<List<TeamUserVO>> listTeams(TeamQuery teamQuery, HttpServletRequest request){
        // 1.边界检测
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        // 2.获取当前登录用户 -> 判断是否是管理员
        User loginUser = userService.getLoginUser(request);
        boolean isAdmin = userService.isAdmin(loginUser);

        // 3.调用Service层查询队伍列表
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, isAdmin);

        /**
         * 4.判断当前用户是否已加入上述队伍 -> 使用stream流遍历
         * 使用 Stream 流提取队伍 ID
         * 流程图解说明：
         * 详细流程请参考：src/main/java/com/yupi/usercentre/common/stream流.png
         * 更多示例请参考：src/main/java/com/yupi/usercentre/common/更多stream示例.png
          */
        final List<Long> teamIdList = teamList.stream().map(TeamUserVO::getId) // TeamUserVO::getId 是指在TeamUserVO类中获取id； 映射/转换 - 对流中的每个元素执行操作，转换成新的元素
                .collect(Collectors.toList());
        // 5.判断当前用户是否已加入队伍
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        try {
            // 如果当前用户登录了，则获取当前用户Id（已在上面获取，直接使用）
            userTeamQueryWrapper.eq("userId",loginUser.getId());
            userTeamQueryWrapper.in("teamId",teamIdList); // teamId必须要在已查询到的队伍列表中
            List<UserTeam> userTeamList = UserTeamService.list(userTeamQueryWrapper);
            // 使用stream流再次取出对应的用户id 加入的 队伍id
            Set<Long> hasJoinTeamIdSet = userTeamList.stream().map(UserTeam::getTeamId).collect(Collectors.toSet());
            // 遍历队伍列表，获取已加入队伍的集合
            teamList.forEach(team -> {
                boolean hasJoin = hasJoinTeamIdSet.contains(team.getId()); // 判断当前用户是否已加入该队伍
                team.setHasJoin(hasJoin); // 设置当前用户是否已加入该队伍
            });
        } catch (Exception e) {
            // 6.查询加入队伍的用户信息(人数)
            QueryWrapper<UserTeam> userTeamJoinQueryWrapper = new QueryWrapper<>();
            userTeamJoinQueryWrapper.in("teamId",teamIdList); // teamId必须要在已查询到的队伍列表中
            List<UserTeam> userTeamList = UserTeamService.list(userTeamJoinQueryWrapper); // 查询所有加入队伍列表中任一队伍的用户
            // 使用stream流对用户加入的队伍分组
            Map<Long, List<UserTeam>> teamIdUserTeamList = userTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
            // 给teamList中每个队伍添加上面查到的用户信息
            teamList.forEach(team -> {
                // getOrDefault(key, defaultValue) 获取指定键对应的值，如果键不存在则返回默认值
                team.setHasJoinNum(teamIdUserTeamList.getOrDefault(team.getId(),new ArrayList<>()).size());
            });
        }
        return ResultUtils.success(teamList);
    }


    // todo 查：分页查询接口 待补充
    @GetMapping("/list/page")
    public BaseResponse<Page<Team>> listTeamsByPage(TeamQuery teamQuery){
        // 1.边界检测
        if (teamQuery == null){
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        // 2.映射 teamQuery --> team
        Team team = new Team();
        BeanUtils.copyProperties(teamQuery, team);
        Page page = new Page(teamQuery.getPageNum(), teamQuery.getPageSize());

        // 3.查询操作
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>(team);
        Page<Team> resultPage = teamService.page(page, queryWrapper);
        return ResultUtils.success(resultPage);
    }


    /**
     * 用户加入队伍
     * @param teamJoinRequest 加入队伍请求参数
     * @param request
     * @return
     */
    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request) {
        // 1.边界检查
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.joinTeam(teamJoinRequest, loginUser);
        return ResultUtils.success(result);
    }


    /**
     * 用户退出队伍
     * @param teamQuitRequest 退出队伍请求参数
     * @param request
     * @return
     */
    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request) {
        // 1.边界检查
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.quitTeam(teamQuitRequest, loginUser);
        return ResultUtils.success(result);
    }


    /**
     * 获取用户创建的队伍列表 -> 根据创建人在team表中的userId查询
     * @param teamQuery 队伍查询参数
     * @param request
     * @return
     */
    @GetMapping("/list/my/create")
    public BaseResponse<List<TeamUserVO>> listMyTeams(TeamQuery teamQuery,HttpServletRequest request){
        // 1.边界检测
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        // 2.获取当前登录用户 -> 获取管理员不需要
        User loginUser = userService.getLoginUser(request);
        // boolean isAdmin = userService.isAdmin(loginUser);
        teamQuery.setUserId(loginUser.getId()); // 直接获取自己的账号信息
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, true); // 默认自己就是管理员
        return ResultUtils.success(teamList);
    }


    /**
     * 获取用户加入的队伍列表
     * @param teamQuery 队伍查询参数
     * @param request
     * @return
     */
    @GetMapping("/list/my/join")
    public BaseResponse<List<TeamUserVO>> listMyJoinTeams(TeamQuery teamQuery,HttpServletRequest request){
        // 1.边界检测
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        // 2.获取当前登录用户 -> 根据登录用户id查询
        User loginUser = userService.getLoginUser(request);
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId",loginUser.getId());
          // 根据查询条件获取账号加入的队伍列表
        List<UserTeam> userTeamList = UserTeamService.list(queryWrapper);
          /*
           去重：取出不重复的队伍id -> 使用stream流
           groupingBy(UserTeam::getTeamId) -> 把整个列表按照队伍id分组
           分成键值对的样子，键是teamId，值是UserTeam对象
           */
        Map<Long, List<UserTeam>> listMap = userTeamList.stream().
                collect(Collectors.groupingBy(UserTeam::getTeamId));
          // 根据分组获得的键值对，获取加入的队伍列表
        List<Long> idList = new ArrayList<>(listMap.keySet());

        // 3.把已经查出的idList作为查询条件，查询队伍列表
        teamQuery.setIdList(idList);
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, true);
        return ResultUtils.success(teamList);
    }


    // ================================= 新功能：聊天接口开始 =================================

    /**
     * 获取用户加入的队伍列表(与上面接口重复了，可以抽象为一个方法了) + 最新聊天信息
     * @param teamQuery
     * @param request
     * @return
     */
    @GetMapping("/list/chat")
    public BaseResponse<List<TeamUserVO>> listChatTeams(TeamQuery teamQuery,HttpServletRequest request) {
        // 1.边界检测
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        // 2.获取当前登录用户 -> 根据登录用户id查询其加入的队伍列表
        User loginUser = userService.getLoginUser(request);
          // 2.1 新建查询，并获取登录用户id + 根据查询条件获取队伍列表
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId",loginUser.getId()); // 根据登录用户id查询
        List<UserTeam> userTeamList = UserTeamService.list(queryWrapper);
        // 2.2 去重，获取用户加入的队伍id，使用stream流,键值对，key是teamId,value是UserTeam对象
        // 然后新建列表存储查到的队伍id的key
        Map<Long, List<UserTeam>> listMap = userTeamList.stream()
                .collect(Collectors.groupingBy(UserTeam::getTeamId));
        ArrayList<Long> idList = new ArrayList<>(listMap.keySet());

        // 3.根据已经查到的idList作为查询条件，查询队伍列表
        teamQuery.setIdList(idList); // 给team查询参数设置idList
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, true); // 然后调用Service层获取当前登录用户的队伍列表

        /* todo
        2.获取当前队伍聊天室未读消息数 -> 基于最后的阅读时间
        3.获取当前队伍聊天室最新信息 -> 查询最新一条信息
         */

        // 4.查询最新一条消息 -> 调用ChatMessageServiceImpl层中的getLastMessage方法
        chatMessageService.getLastMessage(teamQuery.getId());

        // 5.获取未读的消息数
        // 5.1 先获取用户最后的阅读时间(前置条件，先有了最后的阅读时间才可以统计有多少条消息是未读状态)
        Date lastReadTime = chatMessageService.getUserLastReadTime(teamQuery.getId(), loginUser.getId());

        // 5.2 再获取当前队伍聊天室未读消息数 -> 基于最后的阅读时间
        Long unreadCount = chatMessageService.getUnreadCount(teamQuery.getId(), teamQuery.getUserId(), lastReadTime);


        return ResultUtils.success(teamList);
    }


    /**
     * 获取当前队伍聊天室历史消息 (分页)
     * 功能：分页查询某队伍的历史消息，并关联发送者的信息
     * @param chatQuery 聊天消息查询参数
     * @param request 请求
     * @return 聊天消息列表
     */
    @GetMapping("/chat/history")
    public BaseResponse<Page<ChatVO>> getChatHistory(ChatQuery chatQuery, HttpServletRequest request) {
        // 1.边界检查 -> 查询参数不能为空，且队伍id要合法
        if (chatQuery == null || chatQuery.getTeamId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        Long teamId = chatQuery.getTeamId();
        if (teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "队伍id不合法");
        }

        // 2.获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 3.权限校验：验证该用户是否在该队伍中
          // 3.1 新建查询，获取队伍id和用户id
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId",teamId);
        queryWrapper.eq("userId",loginUser.getId());

          // 调用的UserTeamService方法，目的是查询用户——队伍列表中是否存在该用户
        long count = UserTeamService.count(queryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.NO_AUTH,"您不在该队中，无法查看聊天记录");
        }

        // 4.获取队伍聊天室历史消息(分页)
          // 4.1 设置分页默认值 -> 默认页码为1，默认每页10条数据,如果其有数据，按它的来
        Integer pageNum = chatQuery.getPageNum() != null ? chatQuery.getPageNum() : 1;
        Integer pageSize = chatQuery.getPageSize() != null ? chatQuery.getPageSize() : 10;

          // 4.2 调用Service层方法，获取分页的聊天记录
        Page<ChatVO> chatVOPage = chatMessageService.getHistoryMessage(teamId, pageNum, pageSize);

        // 4.3 封装成Page对象，返回给前端，并添加结果校验
        if (chatVOPage == null || chatVOPage.getRecords() == null) {
            // 返回空结果
            return ResultUtils.success(new Page<>());
        }
        // 否则直接返回结果
        return ResultUtils.success(chatVOPage);
    }


    /**
     * 发送聊天消息 -> 先实现发送消息到队伍聊天室的功能，再实现私聊功能
     * @param chatSendRequest 发送消息请求
     * @param request HTTP请求
     * @return 发送消息结果是否成功
     */
    @PostMapping("/chat/send")
    public BaseResponse<ChatVO> sendMessage(@RequestBody ChatSendRequest chatSendRequest, HttpServletRequest request) {
        // 1.边界检查: 请求参数不能为空，队伍id要合法, 并且发送消息不能为空
        if (chatSendRequest == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        Long teamId = chatSendRequest.getTeamId();
        String content = chatSendRequest.getContent();
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "队伍id不合法");
        }
        if (StringUtils.isBlank(content)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "发送消息不可以为空");
        }

        // 并且最多只能发送500个字符
        if (content.length() > 500) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,"发送消息不得超过500字");
        }

        // 2.获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 3.权限检验：验证该用户是否在该队伍中，不在不能发信息
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId",teamId);
        queryWrapper.eq("userId",loginUser.getId());
        long count = UserTeamService.count(queryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.NO_AUTH,"您不在该队中，无法发送消息");
        }

        // 4.在队伍聊天室中发消息
        // 调用Service层方法，发送消息
        ChatVO chatMessageVO = chatMessageService.sendMessage(chatSendRequest, loginUser);

        return ResultUtils.success(chatMessageVO);
    }


    /**
     * 批量标记队伍聊天室消息为已读
     * @param teamId 队伍id
     * @param request 请求
     * @return
     */
    @PostMapping("/chat/read/{teamId}") // 为什么这么写？
    public BaseResponse<Boolean> markMessagesAsRead(@PathVariable Long teamId, HttpServletRequest request) {
        // 1.边界检查
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "队伍id不合法");
        }

        // 2.获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 3.权限检查：验证该用户是否在该队伍中，不在不能看消息和已读消息
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId",teamId);
        queryWrapper.eq("userId",loginUser.getId());
        long count = UserTeamService.count(queryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.NO_AUTH,"您不在该队中，无法查看消息和已读消息");
        }

        // todo 4.批量标记队伍聊天室消息为已读
        chatMessageService.markMessagesAsRead(teamId, loginUser.getId());

        return ResultUtils.success(true);
    }


    // ================================= 新功能：队伍推荐(心动)模式接口开始 =================================

    // 获取推荐队伍列表
    @GetMapping("/recommend")
    public BaseResponse<List<TeamRecommendVO>> getRecommendTeams(@RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, HttpServletRequest request) {
        // 加上@RequestParam注解的意思是：直接从TeamRecommendVO中获取参数pageSize,如果没有该参数，则使用默认值10

        // ========== 步骤1：参数校验 ==========
        if (pageSize == null || pageSize <= 0 || pageSize > 20) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "推荐队伍数量不合法");
        }

        // ========== 步骤2：获取登录用户 ==========
        // 2.1 获取登录用户 并校验
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_LOGIN, "当前未登录");
        }

        // 2.2 获取登录用户的用户Id
        Long loginUserId = loginUser.getId();

        // ========== 步骤3：验证用户是否设置了位置 ==========
        // 3.1 查询登录用户位置信息
        UserLocation currentUserLocation = userLocationService.getCurrentUserLocation(loginUserId);
        if (currentUserLocation == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请先设置您的位置信息");
        }

        // 3.2 验证位置信息完整性 -> 检查位置中的省、市是否存在，并且省、市不能为空
        if (currentUserLocation.getProvince() == null || currentUserLocation.getCity() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "您的位置信息不完整，请重新设置");
        }

        // ========== 步骤4：调用 Service 获取推荐队伍 ==========
        /**
         * 获取推荐队伍列表，实现方法中传入当前登录用户Id和每页展示队伍数量
         */
        List<TeamRecommendVO> recommendTeams = teamService.getRecommendTeams(loginUserId, pageSize);

        // ========== 步骤5：返回结果 ==========
        return ResultUtils.success(recommendTeams);

    }

}
