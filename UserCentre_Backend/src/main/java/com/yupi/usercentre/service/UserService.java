package com.yupi.usercentre.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.usercentre.model.domain.User;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 17832
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-09-06 10:54:59
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册的方法
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @param checkPassword 校验密码
     * @param planetCode 星球(编程导航)编号
     * @return 新用户id
     */
    long userRegister(String userAccount,String userPassword,String checkPassword,String planetCode);


    /**
     * 用户登录的方法
     * @param userAccount
     * @param userPassword
     * @param request 对请求的写入与读取
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户脱敏的方法
     * @param originUser
     * @return 脱敏后的用户信息
     */
    User getSafetyUser(User originUser);

    /**
     * 用户注销的方法，应该是退出登录，不算注销
     * @param request
     * @return
     */
    int userLogout(HttpServletRequest request);


    /**
     * 根据标签搜索用户
     *
     * @param tagNameList
     * @return 搜索到的用户列表
     */
    public List<User> searchUserByTags(List<String> tagNameList);
}


