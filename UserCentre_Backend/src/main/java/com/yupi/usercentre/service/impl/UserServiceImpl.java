package com.yupi.usercentre.service.impl;
import java.util.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yupi.usercentre.common.BaseResponse;
import com.yupi.usercentre.common.ErrorCode;
import com.yupi.usercentre.constant.UserConstant;
import com.yupi.usercentre.exception.BusinessException;
import com.yupi.usercentre.model.domain.User;
import com.yupi.usercentre.model.vo.UserVO;
import com.yupi.usercentre.service.UserService;
import com.yupi.usercentre.mapper.UserMapper;
import com.yupi.usercentre.utils.AlgorithmUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.util.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.yupi.usercentre.constant.UserConstant.USER_LOGIN_STATE;

/**
* @author 17832
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-09-06 10:54:59
*/
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService{

    // 注入UserMapper,可以操作数据库
    @Resource
    private UserMapper userMapper;
    // 盐值：混淆密码
    private static final String SALT = "yupi";



    /**
     * 用户服务的实现类
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @param checkPassword 校验密码
     * @return 新用户id
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword,String planetCode) {

        // 1.校验,使用apache的commons-lang3工具类(Maven)
          // 1.1 参数都不能为空值
        if (StringUtils.isAnyBlank(userAccount,userPassword,checkPassword,planetCode)){
            // todo 修改为自定义异常
            // return -1;
            throw new BusinessException(ErrorCode.PARAM_ERROR,"参数为空");
        }
        if (userAccount.length() < 4){
            // return -1;
            throw new BusinessException(ErrorCode.PARAM_ERROR,"用户账号要大于4位");
        }
        if (planetCode.length() > 8){
            // return -1;
            throw new BusinessException(ErrorCode.PARAM_ERROR,"编程导航编号要小于8位");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8){
            // return -1;
            throw new BusinessException(ErrorCode.PARAM_ERROR,"用户密码和确认密码都必须>=8位");
        }
          // 1.2 账户不能重复，使用queryWrapper查询数据库
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
          // 从数据库拿到对比值，判断是否已存在
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0){
            // return -1;
            throw new BusinessException(ErrorCode.PARAM_ERROR,"账号重复");
        }

          // 1.3 编程导航编号不能重复
        QueryWrapper<User> wrapperCode = new QueryWrapper<>();
        wrapperCode.eq("PlanetCode",planetCode);
        long countCode = userMapper.selectCount(wrapperCode);
        if (countCode > 0){
            // return -1;
            throw new BusinessException(ErrorCode.PARAM_ERROR,"编程导航编号重复");
        }

        // 1.4 账户不包含特殊字符(使用正则表达式)
        String validPattern = "^[a-zA-Z0-9_]+$";
          // 定义一个校验匹配器matcher，校验用户名是否符合正则表达式
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (!matcher.matches()){
            // return -1;
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
          // 1.5 密码和校验密码相同
        if (!userPassword.equals(checkPassword)){
            // return -1;
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        // 2.加密
        // final String SALT = "yupi";
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

        // 3.向用户数据库插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setPlanetCode(planetCode);
        boolean saveResult = this.save(user);
          // 加入判断，保存失败就返回-1
        if (!saveResult) {
            // return -1;
            throw new BusinessException(ErrorCode.PARAM_ERROR,"插入数据失败");
        }
        // 修复：检查id是否回填成功
        if (user.getId() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"注册失败，未获取到用户id");
        }
        return user.getId();
    }


    /**
     * 用户登录的实现类
     * @param userAccount
     * @param userPassword
     * @param request 对请求的写入与读取
     * @return 用户信息
     */
    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {

        // 1.校验,使用apache的commons-lang3工具类(Maven)
        // 1.1 参数都不能为空值
        if (StringUtils.isAnyBlank(userAccount,userPassword)){
            return null;
        }
        if (userAccount.length() < 4){
            return null;
        }
        if (userPassword.length() < 8){
            return null;
        }
        // 1.2 账户不能重复，使用queryWrapper查询数据库
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);

        // 1.3 账户不包含特殊字符(使用正则表达式)
        String validPattern = "^[a-zA-Z0-9_]+$";
        // 定义一个校验匹配器matcher，校验用户名是否符合正则表达式
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (!matcher.matches()){
            return null;
        }

        // 2.加密,要加盐，加盐的目的是防止用户密码被 rainbow table 攻击（相对于把密码混淆）
        // final String SALT = "yupi"; // 最好是直接定义为静态变量
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
          // 查询用户是否存在
          // 兼容：库里是明文或已加密都能匹配，便于当前数据调试
        queryWrapper.and(w -> w.eq("userPassword",encryptPassword)
                              .or()
                              .eq("userPassword",userPassword));
        /*queryWrapper.eq("userAccount",userAccount);
        queryWrapper.eq("userPassword",encryptPassword);*/
        User user = userMapper.selectOne(queryWrapper);
          // 用户不存在
        if (user == null){
            log.info("user login failed,userAccount cannot match userPassword");
            return null;
        }

        // 3.用户脱敏:返回脱敏后的用户信息，只有个别的信息可以被看到
          // 调用getSafetyUser方法
        User safetyUser = getSafetyUser(user);

        // 4.再记录用户的登录状态（登录态里的用户信息也要是脱敏的）
        request.getSession().setAttribute(USER_LOGIN_STATE,safetyUser);
        return safetyUser;
    }


    /**
     * 用户脱敏
     * @param originUser
     * @return 脱敏后的用户信息
     */
    @Override
    public User getSafetyUser(User originUser){
        User safetyUser = new User();
        // 要有判空
        if (originUser == null){
            return null;
        }
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        // safetyUser.setUserPassword("");
        safetyUser.setPhone(originUser.getEmail());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setUserStatus(0);
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setCreateTime(new Date());
        // safetyUser.setUpdateTime(new Date());
        safetyUser.setIsDelete(0);
        safetyUser.setPlanetCode(originUser.getPlanetCode());
        safetyUser.setTags(originUser.getTags());
        return safetyUser;
    }

    /**
     * 用户注销，应该是退出登录，不算注销
     * @param request
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        // 直接移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }


    /**
     * 根据标签，基于SQL搜索用户
     * @param tagNameList
     * @return 搜索到的用户列表
     */
    @Deprecated // 表示此方法已弃用
    private List<User> searchUserByTagsBySQL(List<String> tagNameList) {
        // 如果查询标签为空，直接返回异常信息，否则创建查询
        if (CollectionUtils.isEmpty(tagNameList)){
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        // 法一：SQL查询(实现简单)
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        // 拼接 and查询
          // like '%Java%' and like '%Python%'
        for (String tagName : tagNameList){
            // 循环遍历查询对应的标签页
            queryWrapper = queryWrapper.like("tags",tagName);
        }
        // 返回查询结果，通过userMapper连接Service层和数据库，返回List<User>；(Mapper/Dao层是数据访问层)
        List<User> userList = userMapper.selectList(queryWrapper);

        // 对于userList中每一个user，要脱敏后输出一个新的列表
        return userList.stream().map(user -> {
            return getSafetyUser(user);
        }).collect(Collectors.toList());
    }


    /**
     * 根据标签搜索用户(基于内存过滤)
     * @param tagNameList
     * @return 搜索到的用户列表
     */
    @Override
    public List<User> searchUserByTags(List<String> tagNameList) {
        // 如果查询标签为空，直接返回异常信息，否则创建查询
        if (CollectionUtils.isEmpty(tagNameList)){
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        // 法二：直接在内存中查询(灵活)
        // 1.先查询所有用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        List<User> userList = userMapper.selectList(queryWrapper);
        // 使用Gson反序列化,把JSON转化为java对象
        Gson gson = new Gson();
        // 2.再在内存中判断是否包含要求的标签
        // 2.1 使用stream流的filter方法过滤掉不需要的用户
        return userList.stream().filter(user -> {

            // 2.2 先获取用户的标签
            String tagsStr = user.getTags();
            // 所有用户都要先判断是否有tags
            if (StringUtils.isBlank(tagsStr)){
                return false;
            }
            // 将tagsStr转化为集合set，但Gson不能直接转化为set，需要定义一个类型转化器TypeToken
            Set<String> tempTagNameList = gson.fromJson(tagsStr, new TypeToken<Set<String>>() {
            }.getType());
            // 现在才能反序列化
            // gson.toJson(tempTagNameList);

            // 2.3 遍历用户传入的标签名称TagName
            for (String tagName : tagNameList){
                // 判断用户标签集合中是否包含传入的标签名称tagName,满足则返回true,没有返回false
                if (!tempTagNameList.contains(tagName)){
                    return false;
                }
            }
            return true;
        }).map(user -> {
            return getSafetyUser(user);
        }).collect(Collectors.toList());
        // 上面map部分：stream流的map方法是 对于userList中每一个user，要脱敏后输出一个新的列表
    }


    /**
     * 更新用户信息
     * @param user
     * @param loginUser
     * @return 更新结果
     */
    @Override
    public int updateUser(User user,User loginUser) {
        // 先获取用户id，判断id是否合法(是否<=0)
        Long userId = user.getId();
        // 用户id存在才可以更新
        if (userId <= 0){
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        // todo 补充校验：如果用户没有传任何需要更新的值，直接报错
        // 直接定义一个Service方法，判断用户是否合法
        // 调用hasUpdateFields方法判断是否有更新字段
        if (!hasUpdateFields(user)){
            throw new BusinessException(ErrorCode.PARAM_ERROR,"你没输入要更新的值");
        }

        // 如果是管理员，允许更新任意用户（被合并到下面了）
        /*if (isAdmin(loginUser)){
            // id>0,说明有可能存在,则通过数据库查询到oldUser(Mapper层可以查Service层)
            User oldUser = userMapper.selectById(userId);
            // 2.如果这个查到的用户不存在，则返回错误
            if (oldUser == null){
                throw new BusinessException(ErrorCode.NULL_ERROR);
            }
            // 3.如果用户存在，则更新，先获取其id，然后通过Mapper更新
              // 通过存在的id给要修改的目标user,然后Mapper
            return userMapper.updateById(user);
        }*/

        // 如果不是管理员，只能更新自己信息
        if (!isAdmin(loginUser) && userId != loginUser.getId()){
            // 1.如果不是管理员 -> 且修改的id与自己登录的id不一致，则返回没权限
            if (userId != loginUser.getId()){
                throw new BusinessException(ErrorCode.NO_AUTH);
            }
        }
          // 2.如果是管理员 或者 虽然不是管理员但是id一致，则可以更新，直接按照上面方法修改信息
        // id>0,说明有可能存在,则通过数据库查询到oldUser(Mapper层可以查Service层)
        User oldUser = userMapper.selectById(userId);
            // 2.如果这个查到的用户不存在，则返回错误
        if (oldUser == null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
            // 3.如果用户存在，则更新，先获取其id，然后通过Mapper更新
        // 通过存在的id给要修改的目标user,然后Mapper
        return userMapper.updateById(user);

    }


    /**
     * 获取当前登录用户
     * @param request 请求
     * @return 当前登录用户
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        if (request == null){
            return null;
        }
        // 获取当前登录用户身份状态
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        // 判断当前用户是否有权限
        if (userObj == null){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        return (User)userObj;
    }


    /**
     * 判断当前用户是否是管理员
     * @param loginUser
     * @return true/false
     */
    @Override
    public boolean isAdmin(User loginUser){
        // 仅管理员可查询
        // 1.鉴权
        return loginUser != null && loginUser.getUserRole() == UserConstant.ADMIN_ROLE;
    }


    /**
     * 判断用户是否有更新字段
     * @param user
     * @return true/false
     */
    @Override
    public boolean hasUpdateFields(User user){
        return StringUtils.isNotBlank(user.getUsername()) ||
               StringUtils.isNotBlank(user.getAvatarUrl()) ||
               StringUtils.isBlank(user.getTags()) ||
               // StringUtils.isNotBlank(user.getGender()) || gender是整型，不能用StringUtils.isNotBlank()
               user.getGender() != null ||
               StringUtils.isNotBlank(user.getPhone()) ||
               StringUtils.isNotBlank(user.getEmail());
    }


    /**
     * 找到最匹配的用户
     * @param num 最多返回多少条数据
     * @param loginUser 当前登录用户
     * @return 匹配的用户列表
     */
    @Override
    public List<User> matchUsers(Long num, User loginUser) {

        // 0.创建查询条件，并通过Service层获取通过查询条件的用户列表
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.isNotNull("tags"); // 获取用户列表中不为空的标签
        // queryWrapper.select("id","tags"); // 获取用户列表中的id和tags两列
        List<User> userList = this.list(queryWrapper);

        // 1.获取当前登录用户的标签tags
        String tags = loginUser.getTags();
        // 2.通过Gson将标签字符串转换成List
        Gson gson = new Gson();
        List<String> tagList = gson.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());

        // 3.创建一个距离列表，用于保存用户和标签之间的距离
            // 俩参数：key为用户列表的下标i, value为用户标签之间的距离distance
        // SortedMap<Integer, Long> indexDistanceMap = new TreeMap<>();
        HashMap<Integer, Long> indexDistanceMap = new HashMap<>();
        // 4.遍历userList,并通过Gson将标签字符串转换成List
        for (int i = 0;i < userList.size();i++) {
            // 获取用户列表的下标 -> key为用户列表的下标，value为用户标签之间的距离
            User user = userList.get(i);
            // 每次获取遍历的用户标签
            String userTags = user.getTags();
            // 判断用户标签是否为空 或者 用户标签是登录用户 -> 跳过
            if (StringUtils.isBlank(userTags) || user.getId().equals(loginUser.getId())) {
                continue;
            }
            // 否则转换为列表
            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>() {
            }.getType());

            // 5.计算用户和标签之间的距离 -> 基于AlgorithmUtils中的最短距离算法
            long distance = AlgorithmUtils.minDistance(tagList, userTagList);
            // 6.将距离列表保存到indexDistanceMap中
            indexDistanceMap.put(i, distance);
        }

        // 7.根据距离列表，从userList中取出前num个最匹配的用户
        List<Integer> sortedIndexList = indexDistanceMap.entrySet() // entrySet() -> 获取indexDistanceMap中的全部entry，entry是key-value对
                .stream() // 将entrySet转换成Stream流
                .sorted(Map.Entry.comparingByValue()) // 排序 -> 根据value(距离)进行升序排序，Map.Entry是从Map中获取的entry，comparingByValue是对Value升序排序
                .limit(num) // 获取前num个最匹配的用户
                .map(Map.Entry::getKey) // 获取最匹配的用户的下标 -> 使用map()是因为
                .collect(Collectors.toList());

        // 8.将最匹配的用户列表转为用户VO列表 -> 即返回给前端的是脱敏后的用户列表
        List<User> userVOList = sortedIndexList.stream()
                .map(index -> getSafetyUser(userList.get(index))) // 使用map()提取用户 -> 自动将排序后的index转换为用户，再调用getSafetyUser()进行脱敏
                .collect(Collectors.toList());


        /*List<User> userVOList = new ArrayList<>();
        int i = 0;
        System.out.println("输出 -> 用户id：用户下标：最短距离");
        for (Map.Entry<Integer, Long> entry : indexDistanceMap.entrySet()) {
            if (i > num) {
                break;
            }

            User user = userList.get(entry.getKey());
            System.out.println(user.getId() + ":" + entry.getKey() + ":" + entry.getValue());
            i++;
        }*/
        /*
         7.根据距离列表，从userList中取出前num个最匹配的用户

         indexDistanceMap.keySet().stream().limit(num).collect(Collectors.toList())
         indexDistanceMap.keySet() -> 目的是获取indexDistanceMap中的全部key
         加入stream().limit(num) -> 目的是获取前num个最匹配的用户
         最后使用collect方法将结果转换为List
         */
        /*List<Integer> maxDistanceIndexList = indexDistanceMap.keySet().stream().limit(num).collect(Collectors.toList());

        // 8.将最匹配的用户列表脱敏后返回给前端界面，要使用UserVO
        List<User> userVOList = maxDistanceIndexList.stream()
                .map(index -> {return getSafetyUser(userList.get(index));})
                .collect(Collectors.toList());*/

        return userVOList;
    }



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
    @Override
    public double calculateTagSimilarity(User user1, User user2) {

        // ========== 步骤1：参数校验 ==========
        // 1.1 检验用户对象是否为空
        if (user1 == null || user2 == null) {
            log.info("用户对象的值为空，请检查");
        }

        // 1.2 获取两个用户的标签字段
        String tags1 = user1.getTags();
        String tags2 = user2.getTags();
        if (StringUtils.isBlank(tags1) || StringUtils.isBlank(tags2)) {
            log.info("用户标签为空，请检查");
            return 0;
        }

        // ========== 步骤2：解析标签 ==========
        // 2.1 创建Gson对象，将标签字段从JSON格式转换为String格式
        Gson gson = new Gson();

        // 2.2 将JSON字符串转换为List<String>
        // todo 讲解一下怎么转换的，代码是什么？
        List<String> tagList1 = gson.fromJson(tags1, new TypeToken<List<String>>() {
        }.getType());
        List<String> tagList2 = gson.fromJson(tags2, new TypeToken<List<String>>() {
        }.getType());

        // 2.3 检验解析结果
        if (tagList1 == null || tagList2 == null) {
            log.info("标签解析失败，请重试");
            return 0;
        }

        // ========== 步骤3：计算编辑距离 ==========
        // 3.1 调用算法工具类计算最小距离 -> AlgorithmUtils.misDistance(tagList1, tagList2);
        long distance = AlgorithmUtils.minDistance(tagList1, tagList2);


        // ========== 步骤4：转换为相似度分数 ==========
        // 4.1 计算最大可能距离
        int maxDistance = Math.max(tagList1.size(), tagList2.size());

        // 4.2 计算相似度 -> 归一化：相似度分数 = 1 - 最小距离 / 最大可能距离
        double similarity = 1.0 - (double) distance / maxDistance;

        // 4.3 确保分数在0-1之间 -> Math.max(0.0, x):确保x不小于0，Math.min(1.0,x):确保x不大于1
        double similarityScore = Math.max(0.0, Math.min(1.0, similarity));

        // 4.4 返回相似度分数
        return similarityScore;

    }



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
    @Override
    public double calculateAvgTagSimilarity(User currentUser, List<User> members) {

        // ========== 步骤1：参数校验 ==========
        // 1.1 检查当前登录用户与成员列表是否为空
        if (currentUser == null || members == null || members.isEmpty()) {
            log.info("当前登录用户或队伍列表为空值，请检查");
            return 0;
        }

        // 1.2 检查当前用户是否具有标签
        if (currentUser.getTags() == null || currentUser.getTags().isEmpty()) {
            log.info("当前用户的标签为空，请检查");
        }

        // ========== 步骤2：创建相似度列表 ==========
        // 2.1 创建一个列表，存放每个成员标签页的相似度
        // 为什么类型是Double？ 因为相似度是小数
        ArrayList<Double> similarityList = new ArrayList<>();


        // ========== 步骤3：遍历成员，计算相似度 ==========
        // 3.1 遍历成员列表
        for (User member : members) {
            // 3.2 跳过当前是用户自己的情况
            if (currentUser.getId().equals(member.getId())) {
                continue;
            }

            // 3.3 跳过没有标签的成员
            if (StringUtils.isBlank(member.getTags())) {
                continue;
            }

            // 3.4 计算当前登录用户与遍历到的该成员的标签相似度
            // 调用工具方法calculateTagSimilarity
            double tagSimilarity = calculateTagSimilarity(currentUser, member);

            // 3.5 将相似度添加到标签相似度列表similarityList
            similarityList.add(tagSimilarity);
        }

        // ========== 步骤4：计算平均值 ==========
        // 4.1 判断是否有 有效的相似度
        if (similarityList.isEmpty()) {
            log.info("没有有效的相似度");
            return 0;
        }

        // todo 这一步我没看懂，没看到计算平均值的代码啊？
        // 4.2 使用stream流 计算队伍标签与登录用户相似度 平均值
        // mapToDouble():将Double对象转换为double基本类型
        // average():计算平均值,返回Optional<Double>
        // orElse(0):如果average()返回Optional<Double>为空，则返回0
        double avgSimilarity = similarityList.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);

        // 4.3 返回平均相似度
        return avgSimilarity;

    }
}





