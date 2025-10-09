package com.yupi.usercentre.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类
 * 配置分页插件等功能
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 分页插件配置
     * 作用：拦截分页查询，自动添加 LIMIT 语句并执行 COUNT 查询
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 创建分页拦截器
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        
        // 设置最大单页限制数量，默认 500 条，-1 不受限制
        // 防止恶意请求过大的分页数量
        paginationInnerInterceptor.setMaxLimit(500L);
        
        // 添加分页拦截器
        interceptor.addInnerInterceptor(paginationInnerInterceptor);
        
        return interceptor;
    }
}

