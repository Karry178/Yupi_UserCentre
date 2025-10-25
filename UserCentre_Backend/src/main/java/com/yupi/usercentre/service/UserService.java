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


    /**
     * 更新用户信息
     * @param user
     * @return
     */
    int updateUser(User user,User loginUser);


    /**
     * 获取当前登录用户,从前端请求中获取cookie，然后从session中获取用户信息
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);


    /**
     * 判断当前用户是否是管理员
     * @param loginUser
     * @return true/false
     */
    boolean isAdmin(User loginUser);


    /**
     * 判断当前用户是否是更新字段
     * @param user
     * @return true/false
     */
    boolean hasUpdateFields(User user);


    /**
     * 找到最匹配的用户
     * @param num 匹配用户数量
     * @param loginUser 当前登录用户
     * @return 匹配的用户列表
     */
    List<User> matchUsers(Long num, User loginUser);



    // ============ 新功能：获取最为匹配的队伍列表 =============
    /**
     * 将标签相似度计算抽取成独立方法：
     * 这两个方法属于工具方法，是被其余Service方法调用的，不要直接面向前段！！因为不是独立的业务功能！
     *
     * 原有方法：
     * matchUsers() - 返回匹配用户列表
     *
     * 新增方法：
     * calculateTagSimilarity(User user1, User user2) - 计算两个用户的标签相似度
     * calculateAvgTagSimilarity(User user, List<User> members) - 计算平均相似度
     */


    /**
     * 计算两个用户的标签相似度
     *
     * 算法说明：
     * - 使用编辑距离算法计算标签差异
     * - 将距离转换为相似度分数（0-1之间）
     * - 距离越小，相似度越高
     *
     * 应用场景：
     * - 队伍推荐：计算用户与队伍成员的相似度
     * - 好友推荐：计算用户之间的匹配度
     * - 内容推荐：基于标签推荐相关内容
     *
     * @param user1 用户1
     * @param user2 用户2
     * @return 相似度分数（0-1之间）
     *         - 0.0：完全不相似
     *         - 1.0：完全相同
     *         - 0.7：70%的标签相同
     */
    double calculateTagSimilarity(User user1, User user2);


    /**
     * 计算用户与一组用户的平均标签相似度
     *
     * 算法说明：
     * - 计算当前用户与每个成员的相似度
     * - 求所有相似度的平均值
     * - 跳过当前用户自己和没有标签的成员
     *
     * 应用场景：
     * - 队伍推荐：评估用户与整个队伍的匹配程度
     * - 群组推荐：评估用户与群组的匹配程度
     *
     * @param currentUser 当前用户
     * @param members 成员列表
     * @return 平均相似度分数（0-1之间）
     *         - 如果没有可比较的成员，返回0.0
     */
    double calculateAvgTagSimilarity(User currentUser, List<User> members);
}


