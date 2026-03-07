package ngo.cong.thao.s2o_pro.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    // Đây chính là "khẩu súng" dùng để bắn tin nhắn vào đường ống WebSocket
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Gửi thông báo có đơn hàng mới đến Kênh của Bếp thuộc về một Nhà hàng cụ thể
     */
    public void notifyKitchenNewOrder(String tenantId, String tableName, String orderId) {
        // Cấu trúc URL Kênh (Topic): /topic/tenant/{tenantId}/kitchen
        String destination = "/topic/tenant/" + tenantId + "/kitchen";

        // Gói dữ liệu thành JSON để Front-end dễ đọc
        Map<String, String> payload = Map.of(
                "type", "NEW_ORDER",
                "orderId", orderId,
                "tableName", tableName,
                "message", "🔔 Ting Ting! Bàn " + tableName + " vừa gọi món mới!"
        );

        // Bắn tin nhắn đi
        messagingTemplate.convertAndSend(destination, payload);

        log.info("🚀 Đã bắn thông báo Real-time tới Bếp nhà hàng [{}]. Bàn: {}", tenantId, tableName);
    }
}