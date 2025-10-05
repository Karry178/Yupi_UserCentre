package com.yupi.usercentre;
import java.util.Date;

import com.yupi.usercentre.model.domain.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;

@SpringBootTest
public class RedisTest {

    @Resource
    private RedisTemplate redisTemplate;

    @Test
    void test(){
        // 操作字符串的值——增
        ValueOperations valueOperations = redisTemplate.opsForValue();
        // 操作列表的值
        // ListOperations listOperations = redisTemplate.opsForList();
        valueOperations.set("yupiString","dog");
        valueOperations.set("yupiInt",1);
        valueOperations.set("yupiDouble",2.3);

        User user = new User();
        user.setId(1L);
        user.setUsername("yupi");
        valueOperations.set("yupiUser",user);

        // 查
        Object yupi = valueOperations.get("yupiString");
          // 单元测试
        Assertions.assertTrue("dog".equals((String)yupi));
        yupi = valueOperations.get("yupiInt");
        Assertions.assertTrue(1 == (Integer) yupi);
        yupi = valueOperations.get("yupiDouble");
        Assertions.assertTrue(2.3 == (Double) yupi);
        System.out.println(valueOperations.get("yupiUser"));
    }
}
