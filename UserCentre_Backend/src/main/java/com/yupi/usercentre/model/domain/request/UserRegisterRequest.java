package com.yupi.usercentre.model.domain.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求体
 *
 * 最好继承一下Serializable接口，序列化
 */
@Data
public class UserRegisterRequest implements Serializable {
    /**
     * 请求序列化版本UID！！防止后续升级版本出现问题
     * 但是我的idea目前不能生成UID，后续好好检查一下
     */
    private static final long serialVersionUID = 1L;

    private String userAccount;
    private String userPassword;
    private String checkPassword;

    private String planetCode;

}
