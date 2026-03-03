package ngo.cong.thao.s2o_pro.order.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderEventListener {

    // Hàm này sẽ tự động chạy ngầm khi có đơn hàng PAID
    @EventListener
    public void handleOrderPaidEvent(OrderPaidEvent event) {
        log.info("🔥 [EVENT CATCHED] Đơn hàng {} của nhà hàng {} vừa thanh toán thành công. Tổng tiền: {}.",
                event.orderId(), event.tenantId(), event.amount());

        // Tương lai bạn viết code gửi Notification, gọi API in hóa đơn, hoặc đẩy vào Kafka ở đây!
    }
}