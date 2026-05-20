package com.auction.project.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Cấu hình WebSocket STOMP - thay thế hoàn toàn SocketServer cũ.
 *
 * Cách hoạt động (Observer Pattern qua WebSocket):
 *   1. Client kết nối: GET ws://localhost:8080/ws (SockJS fallback)
 *   2. Client subscribe: /topic/auction/{id}
 *   3. Khi có bid mới: server gọi messagingTemplate.convertAndSend("/topic/auction/{id}", msg)
 *   4. Tất cả client subscribe phiên đó nhận được message tức thì (push, không phải poll)
 *
 * So sánh với thiết kế cũ:
 *   Cũ: SocketServer → ClientHandler → synchronized sendResponse() → Observer.update()
 *   Mới: SimpMessagingTemplate.convertAndSend() → STOMP broker → tất cả subscriber
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefix topic: server gửi tới client
        registry.enableSimpleBroker("/topic", "/queue");
        // Prefix app: client gửi tới server (nếu cần)
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*"); // raw WebSocket cho JavaFX desktop client
    }
}
