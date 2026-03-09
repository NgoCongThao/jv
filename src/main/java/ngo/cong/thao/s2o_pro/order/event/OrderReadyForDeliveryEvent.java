package ngo.cong.thao.s2o_pro.order.event;
import java.util.UUID;
public record OrderReadyForDeliveryEvent(UUID orderId, String tenantId) {}