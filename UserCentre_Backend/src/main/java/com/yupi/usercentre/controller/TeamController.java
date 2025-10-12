package com.yupi.usercentre.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.usercentre.common.BaseResponse;
import com.yupi.usercentre.common.DeleteRequest;
import com.yupi.usercentre.common.ErrorCode;
import com.yupi.usercentre.common.ResultUtils;
import com.yupi.usercentre.model.domain.User;
import com.yupi.usercentre.model.domain.UserTeam;
import com.yupi.usercentre.model.dto.TeamQuery;
import com.yupi.usercentre.exception.BusinessException;
import com.yupi.usercentre.model.domain.Team;
import com.yupi.usercentre.model.request.TeamAddRequest;
import com.yupi.usercentre.model.request.TeamJoinRequest;
import com.yupi.usercentre.model.request.TeamQuitRequest;
import com.yupi.usercentre.model.request.TeamUpdateRequest;
import com.yupi.usercentre.model.vo.TeamUserVO;
import com.yupi.usercentre.service.TeamService;
import com.yupi.usercentre.service.UserService;
import com.yupi.usercentre.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.WebResourceRoot;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 队伍接口
 */
@RestController
@RequestMapping("/team")
// @CrossOrigin 跨域,默认所有的域名都允许跨域访问,可以使用origins={}指定域名
@CrossOrigin(
        origins = "http://localhost:3000",
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
     * @param id
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
    public BaseResponse<List<TeamUserVO>> listTeams(TeamQuery teamQuery,HttpServletRequest request){
        // 1.边界检测
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        // 2.获取当前登录用户 -> 如果登录用户是管理员
        User loginUser = userService.getLoginUser(request);
        boolean isAdmin = userService.isAdmin(loginUser);
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, isAdmin);
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
}
