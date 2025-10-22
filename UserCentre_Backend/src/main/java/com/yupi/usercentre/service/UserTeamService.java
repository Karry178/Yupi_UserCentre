package com.yupi.usercentre.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.usercentre.model.domain.UserTeam;

/**
* @author 17832
* @description 针对表【user_team(用户队伍关系)】的数据库操作Service
* @createDate 2025-10-05 14:50:31
*/
public interface UserTeamService extends IService<UserTeam> {

    /**
     * 判断当前登录用户与指定用户是否是同一队伍的成员
     * @param userId 指定用户Id
     * @param loginUserId 当前登录用户Id
     * @return true-是同一队伍的成员，false-不是同一队伍的成员
     */
    boolean isTeamMember(Long userId, Long loginUserId);
}
