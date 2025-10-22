package com.yupi.usercentre.model.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户位置表(脱敏后) -> 返回用户信息给前端
 * @TableName user_location
 */
@Data
public class UserLocationVO {

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
     * 更新时间
     */
    private Date updateTime;
}