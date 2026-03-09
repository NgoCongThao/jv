package ngo.cong.thao.s2o_pro.delivery.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ngo.cong.thao.s2o_pro.delivery.entity.DeliveryTicket;
import ngo.cong.thao.s2o_pro.delivery.repository.DeliveryTicketRepository;
import ngo.cong.thao.s2o_pro.order.event.OrderReadyForDeliveryEvent;
import ngo.cong.thao.s2o_pro.tenant.TenantContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryEventListener {

    private final DeliveryTicketRepository deliveryTicketRepository;

    @EventListener
    @Transactional
    public void handleOrderReadyEvent(OrderReadyForDeliveryEvent event) {
        // Đảm bảo Context bảo mật được giữ nguyên khi chạy ngầm
        TenantContext.setTenantId(event.tenantId());

        // Kiểm tra xem đơn này đã có vận đơn chưa (tránh tạo trùng)
        if (deliveryTicketRepository.findByOrderId(event.orderId()).isEmpty()) {
            DeliveryTicket ticket = DeliveryTicket.builder()
                    .orderId(event.orderId())
                    .build();
            ticket.setTenantId(event.tenantId());
            deliveryTicketRepository.save(ticket);

            log.info("🛵 [DELIVERY] Đã tự động tạo Vận đơn chờ giao cho Đơn hàng: {}", event.orderId());
        }
        TenantContext.clear();
    }
}