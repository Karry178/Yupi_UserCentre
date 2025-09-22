package com.yupi.usercentre.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yupi.usercentre.common.BaseResponse;
import com.yupi.usercentre.common.ErrorCode;
import com.yupi.usercentre.common.ResultUtils;
import com.yupi.usercentre.exception.BusinessException;
import com.yupi.usercentre.model.domain.User;
import com.yupi.usercentre.model.domain.request.UserLoginRequest;
import com.yupi.usercentre.model.domain.request.UserRegisterRequest;
import com.yupi.usercentre.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

import static com.yupi.usercentre.constant.UserConstant.ADMIN_ROLE;
import static com.yupi.usercentre.constant.UserConstant.USER_LOGIN_STATE;

/*
控制层封装请求：
 */
@RestController
@RequestMapping("/user")
public class UserController {

    // controller层要调用Service层
    @Resource
    private UserService userService;


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
    @GetMapping("/Current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        // 先从session中获取用户的登录态
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        // 把userObj强转为User，作用是获取登录态
        User currentUser = (User) userObj;
        // 判断用户信息，空值就返回null，非空，不要直接返回currentUser，要更新一下再返回给用户，可以再查一次数据库
        if (currentUser == null){
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
        // 逻辑太简单，自己做可以不写入Service层，Controller层写好就行，公司级还是C、S层都要写
        // 调用isAdmin方法判断
        if (!isAdmin(request)){
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
     * 删除用户
     * @param id
     * @param request
     * @return true/false
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(long id,HttpServletRequest request){
        // 调用isAdmin方法判断
        if (!isAdmin(request)){
            return null;
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
     * 鉴权：判断是否是管理员
     * @param request
     * @return true/false
     */
    private boolean isAdmin(HttpServletRequest request){
        // 仅管理员可查询

        // 1.鉴权(仅管理员可以查询)，将登录态作为静态方法放入UserConstant层，访问静态方法要用UserConstant类
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        // 如果用户权限不是1，就不能修改权限，返回空数组
        if (user == null || user.getUserRole() != ADMIN_ROLE){
            return false;
        }
        return true;
    }
}
