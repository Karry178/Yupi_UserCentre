package com.yupi.usercentre.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.usercentre.model.domain.Team;
import com.yupi.usercentre.model.domain.User;
import com.yupi.usercentre.model.dto.TeamQuery;
import com.yupi.usercentre.model.request.TeamJoinRequest;
import com.yupi.usercentre.model.request.TeamQuitRequest;
import com.yupi.usercentre.model.request.TeamUpdateRequest;
import com.yupi.usercentre.model.vo.TeamRecommendVO;
import com.yupi.usercentre.model.vo.TeamUserVO;

import java.util.List;

/**
* @author 17832
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2025-10-05 14:45:57
*/
public interface TeamService extends IService<Team> {

    /**
     * 添加/创建队伍
     * @param team 队伍信息
     * @param loginUser 当前登录用户
     * @return
     */
    long addTeam(Team team, User loginUser);


    /**
     * 搜索队伍列表,并且只有登录后才可以看
     *
     * @param teamQuery 队伍查询参数
     * @param isAdmin 是否为管理员
     * @return
     */
    List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin);


    /**
     * 修改队伍信息
     *
     * @param teamUpdateRequest 队伍信息
     * @param loginUser 当前登录用户
     * @return 修改成功
     */
    boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser);


    /**
     * 加入队伍
     *
     * @param teamJoinRequest 要加入的队伍信息
     * @param loginUser 当前登录用户
     * @return
     */
    boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser);


    /**
     * 退出队伍
     * @param teamQuitRequest
     * @param loginUser
     * @return
     */
    boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser);


    /**
     * 删除队伍
     * @param id
     * @param loginUser
     * @return
     */
    boolean deleteTeam(long id, User loginUser);



    // ================================= 新功能：队伍推荐(心动)模式接口实现 =================================

    /**
     * 推荐队伍列表
     * @param userId 登录用户Id
     * @param pageSize 每页展示队伍数量
     * @return 队伍列表
     */
    List<TeamRecommendVO> getRecommendTeams(Long userId, Integer pageSize);


}
