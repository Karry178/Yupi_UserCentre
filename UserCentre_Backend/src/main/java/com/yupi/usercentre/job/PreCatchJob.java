package com.yupi.usercentre.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.usercentre.model.domain.User;
import com.yupi.usercentre.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 预热任务（预缓存）
 * 1.加入定时缓存功能
 * 2.引入Redisson的分布式锁，保证多线程顺序进行。注意：释放锁要在最后，最好是finally里面
 * 3.引入看门狗机制，避免锁被任意释放，达到续期的目的 --> 开一个监听线程，若方法没执行完，自动续期
 */
@Component
@Slf4j
public class PreCatchJob {

    // 引入UserService，查询数据库，获取用户列表
    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // 引入Redisson的分布式锁 -> RedissonClient
    @Resource
    private RedissonClient redissonClient;


    // 每次预热缓存只去缓存重点用户id为1的人
    private List<Long> mainIdList = Arrays.asList(1L);

    // 每天 23:46:00 缓存推荐用户信息
    @Scheduled(cron = "0 52 10 * * ?")
    public void doCacheRecommendUser(){

        log.info("引入Redisson的分布式锁");
        RLock lock = redissonClient.getLock("yupao:precachejob:docache:lock");
        // 尝试获取锁，最多等待 0 秒(waitTime)，上锁以后 30 秒自动解锁(leaseTime)
        try {
            // 只允许一个线程获取锁
            if(lock.tryLock(0,30000L,TimeUnit.MILLISECONDS)) {
                log.info("获取现在获取锁的线程Id");
                System.out.println("getLock：" + Thread.currentThread().getId());

                // 1.在重点用户列表中循环遍历其中的用户Id
                for (Long userId : mainIdList){
                    QueryWrapper<User> queryWrapper = new QueryWrapper<>();

                    // 定义缓存key的格式，格式为：yupao:user:recommend:userId
                    String redisKey = String.format("yupao:user:recommend:%s", userId);
                    // 定义缓存的
                    ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();

                    // 2.从数据库中查询数据
                    Page<User> userPage = userService.page(new Page<>(1, 10), queryWrapper);

                    // 3.第二步中缓存中没有，就把数据写入缓存
                    try {
                        valueOperations.set(redisKey, userPage, 30, TimeUnit.MINUTES);
                    } catch (Exception e) {
                        log.error("redis set key error", e);
                    }
                }
                log.info("缓存预热结束");

            }
        } catch (InterruptedException e) {
            log.error("doCacheRecommendUser error", e);
        } finally {
            // 释放锁部分
            log.info("解决释放别人锁的问题 -> 只能释放自己的锁");
            if (lock.isHeldByCurrentThread()) {
                // 判断：只有当前线程持有锁，才释放锁
                log.info("释放当前拿到锁的线程Id");
                System.out.println("unLock：" + Thread.currentThread().getId());
                lock.unlock();
            }
        }


    }
}
