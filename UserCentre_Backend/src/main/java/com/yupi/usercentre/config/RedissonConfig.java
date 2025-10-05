package com.yupi.usercentre.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 分布式锁 -> 使用103的Redis 3号库
 * Redisson 配置类
 */
@Configuration
// @ConfigurationProperties是读取yml文件中已有的属性，prefix = "spring.redis"表示从yml文件中读取redis的属性
@ConfigurationProperties(prefix = "spring.redis")
@Data
public class RedissonConfig {

    // 获取Redis在yml文件中的几个属性
    private String host;
    private String port;
    @Value("${spring.redis.password:123456}")
    private String password;

    /**
     * 创建一个Redisson客户端，用于操作Redis
     * 直接从官方文档中复制代码 -> https://redisson.pro/docs/getting-started/
     */
    @Bean
    public RedissonClient redissonClient(){
        // 1.创建Redisson的配置
        Config config = new Config();
        // String redisAddress = "redis://192.168.0.103:6379";  // Redis已经在yml文件配置了，直接调用，不需要再写死
        // 对于Redis的地址，可以使用格式化语法
        String redisAddress = String.format("redis://%s:%s", host, port);

        // useClusterServers 是集群模式，useSingleServer 是单机模式
        SingleServerConfig singleServerConfig = config.useSingleServer().setAddress(redisAddress).setDatabase(3);

        // 如果有密码则设置密码
        if (password != null && password.length() > 0){
            singleServerConfig.setPassword(password);
        }

        // 2.创建实例
        // Sync and Async API -> 同步、异步 API
        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }
}
