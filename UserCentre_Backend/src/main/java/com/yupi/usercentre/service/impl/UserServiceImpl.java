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
        // TODO: 实现用户匹配逻辑

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
}





