package com.yupi.usercentre.model.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 创建新增队伍请求
 * 目的是创建一个新增队伍的封装类，返回给前端特定的参数
 */
@Data
public class TeamUpdateRequest implements Serializable {

    private static final long serialVersionUID = 34263464354676865L;

    /**
     * id
     */
    private Long id;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;


    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 用户id（队长 id）
     */
    private Long userId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

    /**
     * 密码
     */
    private String password;


}
