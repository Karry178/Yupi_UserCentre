package com.yupi.usercentre.constant;

/**
 * 用户常量类接口，默认是Public Static
 */
public interface UserConstant {

    /**
     * 定义一个用户登录状态的key（全局）
     */
    String USER_LOGIN_STATE = "userLoginState";

    /**
     * 定义管理员角色与普通用户角色 权限
     */
    int ADMIN_ROLE = 1;
    int DEFAULT_ROLE = 0;
}
