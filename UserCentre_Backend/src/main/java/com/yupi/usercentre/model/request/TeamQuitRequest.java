package com.yupi.usercentre.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建用户退出队伍请求
 * 目的是创建一个用户退出队伍的封装类，返回给前端特定的参数
 */
@Data
public class TeamQuitRequest implements Serializable {

    private static final long serialVersionUID = 34263464354676865L;

    /**
     * id
     */
    private Long teamId;

}
