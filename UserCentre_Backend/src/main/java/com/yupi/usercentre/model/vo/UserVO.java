package com.yupi.usercentre.model.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;

import java.util.Date;

/**
 * 用户包装类（脱敏）
 * VO 是 View Object 的简称，即返回给前端的数据对象
 *
 * @author 17832
 */
public class UserVO {

    /**
     * 学号
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 登录账号
     */
    private String userAccount;

    /**
     * 标签列表
     */
    private String tags;

    /**
     * 用户头像
     */
    private String avatarUrl;

    /**
     * 性别
     */
    private Integer gender;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 用户账号状态
     */
    private Integer userStatus;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     *
     * 要加@TableLogic,目的是逻辑删除
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 用户权限 0 - 普通用户，1 - 管理员
     */
    private Integer userRole;

    /**
     * 编程导航编号
     */
    private String planetCode;
}
