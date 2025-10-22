package com.yupi.usercentre.model.vo;

import lombok.Data;
import java.util.Date;


/**
 * 返回队伍成员位置 + 距离信息  发给前端(脱敏后的)
 */
@Data
public class TeamMemberDistanceVO {

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 城市
     */
    private String city;

    /**
     * 省份
     */
    private String province;

    /**
     * 距离
     */
    private Double distance;

    /**
     * 发送时间
     */
    private Date createTime;
}
