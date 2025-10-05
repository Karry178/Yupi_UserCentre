package com.yupi.usercentre;

import com.yupi.usercentre.model.domain.User;
import com.yupi.usercentre.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@SpringBootTest
public class insertUsersTest {

    // Bean类使用Mapper管理更方便
    @Resource
//    private UserMapper userMapper;
    // 使用Service类可以批量插入数据
    private UserService userService;

    /**
     * 批量插入用户
     */
    @Test
    public void doInsertUsers1(){
        // StopWatch是监控类，可以统计任务使用时间
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int INSERT_NUM = 1000;
        List<User> userList = new ArrayList<>();
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
            // userMapper.insert(user);

        }
        // 使用Service类可以批量插入数据
        // 20秒大概可以插入10万条数据
        userService.saveBatch(userList,100);
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }


    /**
     * 并发批量插入用户——>线程池
     * 总共插入10万条，一次一万
     */
    @Test
    public void doInsertUsers2(){
        // StopWatch是监控类，可以统计任务使用时间
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int INSERT_NUM = 100000;

        int batchSize = 5000; // 每次插入5000条数据

        // 分10次插入
        int j = 0;
        // 定义一个异步任务数组,方便下面异步插入数据
        List<CompletableFuture<Void>> futureList = new ArrayList<>();

        for (int i = 0;i < 20;i++){
            List<User> userList = new ArrayList<>();
            while (true){
                j++;
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
                userList.add(user);
                // 如果j满足了10000条，跳出循环，插入这一万条，进行下一次循环插入
                if (j % 10000 == 0){
                    break;
                }
            }

            // 使用异步操作插入数据
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                System.out.println("threadName:" + Thread.currentThread().getName());
                // 使用Service类可以批量插入数据
                userService.saveBatch(userList, batchSize);
            });
            // 把拿到的10个异步任务 添加到异步任务数组中
            futureList.add(future);
        }
        // 等待所有任务完成后，才执行下一条语句stop，不加join依旧是异步的，会直接执行下一行
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[]{})).join();

        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }
}
