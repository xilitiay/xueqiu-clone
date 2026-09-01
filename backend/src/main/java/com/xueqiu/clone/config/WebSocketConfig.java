package com.xueqiu.clone.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;

/**
 * WebSocket 实时行情推送配置（SockJS + STOMP）。
 *
 * - 端点 /ws：前端通过 SockJS 建立长连接，再走 STOMP 协议订阅主题。
 *   SockJS 在原生 WebSocket 不可用时自动降级为 HTTP 流/轮询，兼容性与雪球一致。
 * - 主题：
 *     /topic/quotes   批量个股实时行情（Map<symbol, QuoteDTO>）
 *     /topic/indices  市场指数实时行情（List<IndexDTO>）
 *   由 QuotePushScheduler 每 5 秒推送一次；前端订阅后即可实时刷新，无需轮询 REST。
 * - 允许跨域源复用 app.cors.allowed-origins（前端 Vite 开发服务器），避免握手被浏览器拦截。
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins.split(","))
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 服务端向客户端推送的主题前缀
        registry.enableSimpleBroker("/topic", "/queue");
        // 客户端发往服务端的消息前缀（如 /app/xxx），本演示以服务端单向推送为主
        registry.setApplicationDestinationPrefixes("/app");
        // 点对点消息前缀（预留，便于后续做「某用户持仓预警」等单播）
        registry.setUserDestinationPrefix("/user");
    }
}
