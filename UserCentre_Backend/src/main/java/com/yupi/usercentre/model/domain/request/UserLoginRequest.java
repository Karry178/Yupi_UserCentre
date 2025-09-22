package com.yupi.usercentre.model.domain.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求体
 */
@Data
public class UserLoginRequest implements Serializable {
    // 生成序列化ID
    private static final long serialVersionUID =  34553456457547L;

    private String userAccount;
    private String userPassword;

}
