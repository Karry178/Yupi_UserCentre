package com.yupi.usercentre.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.usercentre.common.BaseResponse;
import com.yupi.usercentre.common.ErrorCode;
import com.yupi.usercentre.common.ResultUtils;
import com.yupi.usercentre.exception.BusinessException;
import com.yupi.usercentre.model.domain.User;
import com.yupi.usercentre.model.request.UserLoginRequest;
import com.yupi.usercentre.model.request.UserRegisterRequest;
import com.yupi.usercentre.model.vo.UserVO;
import com.yupi.usercentre.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.util.CollectionUtils; // Spring Framework 提供的工具类


/**
 * 用户接口
 */
@RestController
@RequestMapping("/user")
// @CrossOrigin 跨域,默认所有的域名都允许跨域访问,可以使用origins={}指定域名
@CrossOrigin(
        origins = "http://localhost:3000",
        allowCredentials = "true"
)
@Slf4j
public class UserController {

    // controller层要调用Service层,引入Service层
    @Resource
    private UserService userService;

    // 引入RedisTemplate
    @Resource
    private RedisTemplate<String,Object> redisTemplate;


    /**
     * 用户注册的Controller方法
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> UserRegister(@RequestBody UserRegisterRequest userRegisterRequest){
        if (userRegisterRequest == null){
            // return ResultUtils.error(ErrorCode.PARAM_ERROR);
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        // 不为空，就返回一个id。调用Service层方法，修改数据库
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String planetCode = userRegisterRequest.getPlanetCode();
        // 添加一个条件，三个只要有一个空值，就返回-1
        if (StringUtils.isAnyBlank(userAccount,userPassword,checkPassword,planetCode)){
            return null;
        }
          // 只有上面几个基础逻辑都通过，才可以调用Service层
        long result = userService.userRegister(userAccount, userPassword, checkPassword,planetCode);
        return ResultUtils.success(result);
    }


    /**
     * 用户登录的Controller方法
     * @param userLoginRequest
     * @param request
     * @return 登录的用户
     */
    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request){
        if (userLoginRequest == null){
            return null;
        }
        // 不为空，就返回一个id。调用Service层方法，修改数据库
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        // 添加一个条件，三个只要有一个空值，就返回-1
        if (StringUtils.isAnyBlank(userAccount,userPassword)){
            return null;
        }
        // 只有上面几个基础逻辑都通过，才可以调用Service层
        User user = userService.userLogin(userAccount, userPassword,request);
        // 关键步骤：设置session，把用户信息保存在session中
        request.getSession().setAttribute("user",user);
        return ResultUtils.success(user);
    }

    // 用户注销/退出登录
    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request){
        if (request == null){
            return null;
        }
        int i = userService.userLogout(request);
        return ResultUtils.success(i);
    }


    /**
     * 获取用户的登录状态
     * @param request
     * @return 一个脱敏后的用户id信息
     */
    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        System.out.println("收到current的请求");

        // 先从session中获取用户的登录态
        Object userObj = request.getSession().getAttribute("user");
        System.out.println("session中的用户：" + userObj);

        // 把userObj强转为User，作用是获取登录态
        User currentUser = (User) userObj;
        // 判断用户信息，空值就返回null，非空，不要直接返回currentUser，要更新一下再返回给用户，可以再查一次数据库
        if (currentUser == null){
            System.out.println("session中没有用户信息");
            return null;
        }
        // 要用id查询用户，返回一个脱敏后的用户信息
        Long userId = currentUser.getId();
        // todo 校验用户是否合法 (todo用处是待办事项)
        User user = userService.getById(userId);
        User safetyUser = userService.getSafetyUser(user);
        return ResultUtils.success(safetyUser);
    }

    /**
     * 通过username查询多用户
     * @param username
     * @return 用户列表
     */
    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String username,HttpServletRequest request){
        // *先从Service层获取登录状态，只有登录了才能进行删除
        User loginUser = userService.getLoginUser(request);
        // 调用isAdmin方法判断
        if (!userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH,"用户不是管理员，无权限查询");
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(username)){
            // 如果username不为空，就模糊查询即可(会查找所有包含username的)
            queryWrapper.like("username",username);
        }
        // 最后调用userService层方法连接数据库
        List<User> userList = userService.list(queryWrapper);
          // 获取用户列表，但是每一个都将密码返回空值(脱敏)
        List<User>  list = userList.stream().map(user -> {
            // 用户脱敏
            return userService.getSafetyUser(user);
        }).collect(Collectors.toList());
        return ResultUtils.success(list);
    }


    /**
     * 通过标签查询多用户
     * @param tagNameList
     * @return 用户列表
     */
    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUsersByTags(@RequestParam(required = false) List<String> tagNameList){
        // 判断：标签为空，直接返回参数异常
        if (CollectionUtils.isEmpty(tagNameList)){
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        List<User> userList = userService.searchUserByTags(tagNameList);
        return ResultUtils.success(userList);
    }


    // todo 推荐多个用户 未匹配
    /**
     * 主页推荐多个用户
     * @param pageSize
     * @param pageNum
     * @param request
     * @return
     */
    @GetMapping("/recommend")
    public BaseResponse<Page<User>> recommendUsers(long pageSize, long pageNum,HttpServletRequest request){
        // 获取当前登录的用户id
        User loginUser = userService.getLoginUser(request);
        // 定义缓存key的格式，格式为：yupao:user:recommend:userId
        String redisKey = String.format("yupao:user:recommend:%s", loginUser.getId());
        // 定义缓存的
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();

        // 1.先判断有无缓存，有就直接读缓存
          // 获取缓存中的数据
        Page<User> userPage = (Page<User>)valueOperations.get(redisKey);
        if (userPage != null){
            return ResultUtils.success(userPage);
        }

        // 2.缓存中没有，再从数据库中查询
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        // 调用userService层方法连接数据库进行分页查询
        userPage = userService.page(new Page<>(pageNum, pageSize), queryWrapper);

        // 3.将数据写入缓存，设置30分钟过期时间
        try {
            valueOperations.set(redisKey, userPage, 30, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("redis set key error", e);
        }


        // 对分页结果中的每个用户进行脱敏处理
        List<User> safetyUserList = userPage.getRecords().stream().map(user -> {
            // 用户脱敏
            return userService.getSafetyUser(user);
        }).collect(Collectors.toList());
        
        // 重新设置脱敏后的用户列表到分页对象中
        userPage.setRecords(safetyUserList);
        
        return ResultUtils.success(userPage);
    }


    /**
     * 更新用户信息
     * @param user 用户
     * @param request 前端请求
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Integer> updateUser(@RequestBody User user,HttpServletRequest request){
        // 1.校验参数是否为空
        if (user == null){
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        // 在Controller层也获取一次登录态,内外层各一个判断;外层调用内层的判断，内层为UserServiceImpl层getLoginUser方法中
        User loginUser = userService.getLoginUser(request);
         // 2.触发更新
        int result = userService.updateUser(user,loginUser);
        return ResultUtils.success(result);
    }


    /**
     * 删除用户
     * @param id
     * @param request
     * @return true/false
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestParam long id, HttpServletRequest request){
        // *先从Service层获取登录状态，只有登录了才能进行删除
        User loginUser = userService.getLoginUser(request);
        // 然后才可以调用isAdmin方法判断
        if (!userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH,"无权限删除");
        }
        // id不能为空值
        if (id <= 0){
            return null;
        }
        // 删除直接调用Service层的remove方法
        boolean remove = userService.removeById(id);
        return ResultUtils.success(remove);
    }


    /**
     * 获取最为匹配的用户
     * @param num 匹配用户数量
     * @param request 前端请求
     * @return 返回匹配的用户列表(脱敏后的User)
     */
    @GetMapping("/match")
    public BaseResponse<List<User>> matchUsers(Long num, HttpServletRequest request){
        // 1.限制查询到的用户数量！(防止无限制查询，窃取数据库内容)
        if (num <= 0 || num > 20) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        // 2.如果满足条件，获取当前登录用户后，调用Service层方法进行匹配
        User loginUser = userService.getLoginUser(request);
        List<User> userList = userService.matchUsers(num, loginUser);
        return ResultUtils.success(userList);
    }
}
