package com.yupi.usercentre.model.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 创建用户加入队伍请求
 * 目的是创建一个用户加入队伍的封装类，返回给前端特定的参数
 */
@Data
public class TeamJoinRequest implements Serializable {

    private static final long serialVersionUID = 34263464354676865L;

    /**
     * id
     */
    private Long teamId;


    /**
     * 密码
     */
    private String password;


}
