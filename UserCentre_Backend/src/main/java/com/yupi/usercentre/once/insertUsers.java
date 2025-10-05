package com.yupi.usercentre.once;

import com.yupi.usercentre.mapper.UserMapper;
import com.yupi.usercentre.model.domain.User;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;

@Component // 将类交给spring管理
public class insertUsers {

    // Bean类使用Mapper管理更方便
    @Resource
    private UserMapper userMapper;

    /**
     * 批量插入用户
     */
//    @Scheduled(fixedDelay = 5000, fixedRate = Long.MAX_VALUE)
    public void doInsertUsers(){
        // StopWatch是监控类，可以统计任务使用时间
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int INSERT_NUM = 1000;
        for (int i = 0;i < INSERT_NUM;i++){
            User user = new User();
            user.setUsername("假用户");
            user.setUserAccount("fakeNews");
            user.setTags("[]");
            user.setAvatarUrl("");
            user.setGender(0);
            user.setUserPassword("12345678");
            user.setPhone("12345");
            user.setEmail("123@123.com");
            user.setUserStatus(0);
            user.setUserRole(0);
            user.setPlanetCode("1111");

            // 使用Mapper遍历插入数据
            userMapper.insert(user);
        }
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }
}
