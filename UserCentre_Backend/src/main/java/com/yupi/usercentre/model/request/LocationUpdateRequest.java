package com.yupi.usercentre.model.request;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户更新位置表
 * @TableName user_location
 *
 * 用户可以更新 城市、省份、经纬度、详细地址、位置类型、隐私设置
 *
 * 【优化】可以在DTO中添加交验注解，推荐使用 @Valid 注解
 * 加入@Valid注解后，框架会自动进行参数校验，并返回错误信息。
 */
@Data
public class LocationUpdateRequest {

    /**
     * 城市
     */
    @NotNull(message = "城市不能为空")
    private String city;

    /**
     * 省份
     */
    private String province;

    /**
     * 经度
     */
    @NotNull(message = "经度不能为空")
    @DecimalMin(value = "-180.0", message = "精度范围：-180~180")
    @DecimalMax(value = "180.0", message = "精度范围：-180~180")
    private Double longitude;

    /**
     * 纬度
     */
    @NotNull(message = "纬度不能为空")
    @DecimalMin(value = "-90.0", message = "精度范围：-90~90")
    @DecimalMax(value = "90.0", message = "精度范围：-90~90")
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

}