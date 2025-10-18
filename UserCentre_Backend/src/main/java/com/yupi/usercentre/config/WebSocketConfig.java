package com.yupi.usercentre.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;


/**
 * WebSocket配置类
 * 作用：
 * 1. 启用 WebSocket 功能
 * 2. 注册 WebSocket 处理器
 * 3. 配置 WebSocket 端点（URL）
 *
 * WebSocket作用是：实现服务器与客户端之间的长连接，实现实时通信
 */
@Configuration // 配置类
@EnableWebSocket // 开启WebSocket支持
public class WebSocketConfig {

    /**
     * 注入 ServerEndpointExporter
     * 这个 Bean 会自动注册使用 @ServerEndpoint 注解的类
     * @return
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }



    /**
     * 注册WebSocket处理器
     * @Param registry WebSocket 处理器注册器
     */
    /*@Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat") // 注册处理器和端点
                .setAllowedOrigins("*"); // 允许所有源访问(允许跨域)
    }*/


}
