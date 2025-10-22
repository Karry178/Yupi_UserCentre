package com.yupi.usercentre.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 用户位置表
 * @TableName user_location
 */
@TableName(value ="user_location")
@Data
public class UserLocation {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
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
     * 经度
     */
    private Double longitude;

    /**
     * 纬度
     */
    private Double latitude;

    /**
     * 详细地址
     */
    private String address;

    /**
     * 位置类型：0-当前位置，1-常住地址
     */
    private Integer locationType;

    /**
     * 隐私设置
     */
    private Integer locationPrivacy;

    /**
     * 
     */
    private Date createTime;

    /**
     * 
     */
    private Date updateTime;
}