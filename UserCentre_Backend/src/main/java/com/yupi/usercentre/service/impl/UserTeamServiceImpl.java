package com.yupi.usercentre.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.usercentre.model.domain.UserTeam;
import com.yupi.usercentre.mapper.UserTeamMapper;
import com.yupi.usercentre.service.UserTeamService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


/**
* @author 17832
* @description 针对表【user_team(用户队伍关系)】的数据库操作Service实现
* @createDate 2025-10-05 14:50:31
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService{

    /**
     * 判断当前用户是否是队伍成员
     * @param userId 指定用户Id
     * @param loginUserId 当前登录用户Id
     * @return true/false
     */
    @Override
    public boolean isTeamMember(Long userId, Long loginUserId) {
        // 1.查询当前登录用户加入的所有队伍
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", loginUserId);
        queryWrapper.select("teamId"); // 只查询队伍Id
        List<UserTeam> loginUserTeamList = this.list(queryWrapper);

        // 2.提取登录用户加入的队伍Id列表
        List<Long> teamIds = loginUserTeamList.stream().map(UserTeam::getTeamId)
                .collect(Collectors.toList());

        // 3.判断：如果当前登录用户没有加入任何队伍，返回false
        if (teamIds.isEmpty()) {
            return false;
        }

        // 4.查询要比较的用户是否也在当前这些队伍中
        QueryWrapper<UserTeam> queryWrapper1 = new QueryWrapper<>();
        queryWrapper1.eq("userId", userId);
        queryWrapper1.in("teamId", teamIds);

        long count = this.count(queryWrapper1);

        // 5.如果count > 0,说明有公共队伍
        if (count > 0) {
            return true;
        }
        return false;
    }
}




