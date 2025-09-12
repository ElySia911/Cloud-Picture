package com.oy.oypicturebackend.manager.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import javax.annotation.Resource;

/**
 * WebSocket 配置（定义连接）
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Resource
    PictureEditHandler pictureEditHandler;//处理器
    @Resource
    private WsHandshakeInterceptor wsHandshakeInterceptor;//拦截器

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(pictureEditHandler, "/ws/picture/edit")//当前端访问 /ws/picture/edit 时，用pictureEditHandler处理所有的WebSocket连接和消息
                .addInterceptors(wsHandshakeInterceptor)//添加拦截器
                .setAllowedOrigins("*");
    }
}
