package com.yupi.usercentre.service.impl;

import com.yupi.usercentre.exception.BusinessException;
import com.yupi.usercentre.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@SpringBootTest
@Transactional
@Rollback
class UserServiceImplTest {

    @Resource
    private UserService userService;

    @Test
    void userRegister() {
        // 1.空密码（空白）用例应失败
        Assertions.assertThrows(BusinessException.class, () -> {
            userService.userRegister("yupi_case1", " ", "123456", "123");
        });

        // 2.测试账户长度<4位
        Assertions.assertThrows(BusinessException.class, () -> {
            userService.userRegister("yi", "12345678", "12345678", "123");
        });

        // 3.测试密码长度小于8位
        Assertions.assertThrows(BusinessException.class, () -> {
            userService.userRegister("yupi", "123456", "123456", "123");
        });

        // 4.测试账户是否包含特殊字符
        Assertions.assertThrows(BusinessException.class, () -> {
            userService.userRegister("y p", "12345678", "12345678", "123");
        });

        // 5.测试密码与校验密码是否一致
        Assertions.assertThrows(BusinessException.class, () -> {
            userService.userRegister("validuser", "12345678", "123456789", "123");
        });

        // 6.测试账户是否存在（假设dog账户已存在）
        Assertions.assertThrows(BusinessException.class, () -> {
            userService.userRegister("dog", "123456789", "123456789", "123");
        });

        // 7.测试正常注册（使用唯一账号避免和数据库或前序用例冲突）
        String uniqueAccount = "yupi_unique_" + System.currentTimeMillis();
        long result = userService.userRegister(uniqueAccount, "12345678", "12345678", "1234");
        Assertions.assertTrue(result > 0); // 生成的id大于0
    }
}
