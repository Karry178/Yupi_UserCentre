package com.yupi.usercentre.service;

import com.yupi.usercentre.model.domain.UserLocation;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.usercentre.model.vo.TeamMemberDistanceVO;

import java.util.List;

/**
* @author 17832
* @description 针对表【user_location(用户位置表)】的数据库操作Service
* @createDate 2025-10-20 18:34:52
*/
public interface UserLocationService extends IService<UserLocation> {


    List<TeamMemberDistanceVO> getTeamMembersLocation(Long teamId, Double maxDistance, Long userId);


    UserLocation updatedLocation(UserLocation userLocation);


    UserLocation getCurrentUserLocation(Long loginUserId);


    /**
     * 获取指定用户的位置信息
     * @param userId 指定的用户id
     * @return 该用户位置信息
     */
    UserLocation getTheUserLocation(Long userId);
}
