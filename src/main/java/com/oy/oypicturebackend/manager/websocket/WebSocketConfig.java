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
@EnableWebSocket//开启 Spring 对 WebSocket 的支持
public class WebSocketConfig implements WebSocketConfigurer {

    @Resource
    PictureEditHandler pictureEditHandler;//处理器
    @Resource
    private WsHandshakeInterceptor wsHandshakeInterceptor;//拦截器

    //访问/ws/picture/edit路径的 WebSocket 请求，必须先过wsHandshakeInterceptor拦截器，再交给pictureEditHandler处理器
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(pictureEditHandler, "/ws/picture/edit")
                .addInterceptors(wsHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
