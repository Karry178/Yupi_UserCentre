 package com.yupi.usercentre.service;

 import com.yupi.usercentre.model.domain.User;
 import org.junit.jupiter.api.Test;
 import org.springframework.boot.test.context.SpringBootTest;
 import org.springframework.test.annotation.Rollback;
 import org.springframework.transaction.annotation.Transactional;

 import javax.annotation.Resource;

 import static org.junit.jupiter.api.Assertions.*;

 // 用户服务测试
 @SpringBootTest
 @Transactional
 @Rollback
 class UserServiceTest {

     @Resource
     private UserService userService;

     @Test
     void testAddUser(){
         User user = new User();

         user.setUsername("dogyupi");
         user.setUserAccount("1010");
         user.setAvatarUrl("https://img2.baidu.com/it/u=3422245222,2832823222&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500");
         user.setGender(0);
         user.setUserPassword("123456");
         user.setPhone("1234");
         user.setEmail("4321@qq.com");

         boolean result = userService.save(user);
         System.out.println(user.getId());
         // 加入断言,result为true才表示测试通过,否则表示测试失败
         assertTrue(result);
     }

 }