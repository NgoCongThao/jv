package ngo.cong.thao.s2o_pro.order.event;

import java.math.BigDecimal;
import java.util.UUID;

// Dùng Record của Java để tạo đối tượng chứa dữ liệu (Immutable) nhanh gọn
public record OrderPaidEvent(UUID orderId, BigDecimal amount, String tenantId) {}