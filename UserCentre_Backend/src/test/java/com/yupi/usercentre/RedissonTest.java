package com.yupi.usercentre;

import org.junit.jupiter.api.Test;
import org.redisson.api.RList;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class RedissonTest {

    @Resource
    private RedissonClient redissonClient;

    @Test
    void test(){
        // list自身的操作方法：数据存在本地JVM内存中
        List<String> list = new ArrayList<>();
        list.add("yupi");
        System.out.println("list" + list.get(0));
        // list.remove(0);

        // Redisson 的list操作方法：数据存在Redis的内存中
        RList<String> rList = redissonClient.getList("test-list");
        // rList.add("yupi");
        System.out.println("rList" + rList.get(0));
         rList.remove(0);


        // map
        Map<String, Integer> map = new HashMap<>();
        map.put("yupi",10);
        map.get("yupi");

        RMap<String, Integer> rMap = redissonClient.getMap("test-map");
        rMap.put("yupi",10);
    }


    /**
     * 测试Redisson看门狗功能
     * 自动续期机制 --> 1.监听当前线程，每10s续期一次；
     *                  2.如果当前线程挂掉(注意debug模式也会被当做服务器宕机！)，则不会续期
     */
    @Test
    void setrWatchDog(){
        RLock lock = redissonClient.getLock("yupao:precatchjob:docache:lock");
        // 只有一个线程可以获取到锁
        try {
            if (lock.tryLock(0,-1, TimeUnit.MILLISECONDS)) {
                // 加一个睡眠机制 -> 无限睡，设置100s
                Thread.sleep(100000);
                System.out.println("getLock:" + Thread.currentThread().getId());
            }
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        } finally {
            // 释放锁操作，只允许释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                System.out.println("unLock:" + Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }
}
