package ngo.cong.thao.s2o_pro.config; // Hoặc package websocket tùy bạn chọn

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // Bật tính năng Trạm trung chuyển tin nhắn
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Đây là điểm vào (endpoint) mà Front-end sẽ dùng để mở đường ống kết nối
        // Ví dụ Front-end sẽ gọi: new SockJS("http://localhost:8080/ws")
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Cho phép mọi domain kết nối (Tạm thời mở để test)
                .withSockJS(); // Fallback: Nếu trình duyệt cũ không hỗ trợ WebSocket thì dùng SockJS thay thế
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Cấu hình tiền tố cho các "Kênh" (Topic) mà Server sẽ đẩy tin nhắn về cho Client
        // Bếp hoặc Thu ngân sẽ "Subscribe" (Đăng ký theo dõi) các kênh bắt đầu bằng /topic
        registry.enableSimpleBroker("/topic");

        // Cấu hình tiền tố cho các tin nhắn từ Client gửi lên Server (nếu có)
        registry.setApplicationDestinationPrefixes("/app");
    }
}