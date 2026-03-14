package ngo.cong.thao.s2o_pro.order.service;

import ngo.cong.thao.s2o_pro.order.entity.OrderStatus;
import ngo.cong.thao.s2o_pro.order.entity.OrderType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Component
public class OrderStateEngine {

    // Chứa bản đồ các bước chuyển trạng thái hợp lệ
    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        // --- THÊM DÒNG NÀY ĐỂ MỞ LUỒNG THANH TOÁN TRƯỚC ---
        VALID_TRANSITIONS.put(OrderStatus.PENDING_PAYMENT, Set.of(OrderStatus.NEW, OrderStatus.CANCELLED));

        // Mở thêm luồng cho đơn ăn tại bàn (DINE_IN) thanh toán ngay lúc mới quét mã tạo đơn
        VALID_TRANSITIONS.put(OrderStatus.NEW, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED, OrderStatus.PAID));

        VALID_TRANSITIONS.put(OrderStatus.CONFIRMED, Set.of(OrderStatus.COOKING, OrderStatus.PREPARING, OrderStatus.CANCELLED));
        VALID_TRANSITIONS.put(OrderStatus.COOKING, Set.of(OrderStatus.DONE));
        VALID_TRANSITIONS.put(OrderStatus.PREPARING, Set.of(OrderStatus.READY));
        VALID_TRANSITIONS.put(OrderStatus.READY, Set.of(OrderStatus.OUT_FOR_DELIVERY));
        VALID_TRANSITIONS.put(OrderStatus.DONE, Set.of(OrderStatus.PAID, OrderStatus.COOKING));
        VALID_TRANSITIONS.put(OrderStatus.OUT_FOR_DELIVERY, Set.of(OrderStatus.DELIVERED));
        VALID_TRANSITIONS.put(OrderStatus.DELIVERED, Set.of(OrderStatus.PAID));
// Khách có thể gọi tính tiền ngay khi vừa order (NEW), đang ăn (SERVED), hoặc ăn xong (DONE)
        VALID_TRANSITIONS.put(OrderStatus.NEW, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED, OrderStatus.PAID, OrderStatus.PAYMENT_REQUESTED));
        VALID_TRANSITIONS.put(OrderStatus.SERVED, Set.of(OrderStatus.DONE, OrderStatus.PAYMENT_REQUESTED));
        VALID_TRANSITIONS.put(OrderStatus.DONE, Set.of(OrderStatus.PAID, OrderStatus.COOKING, OrderStatus.PAYMENT_REQUESTED));
        VALID_TRANSITIONS.put(OrderStatus.PAID, Set.of());

        VALID_TRANSITIONS.put(OrderStatus.PAYMENT_REQUESTED, Set.of(OrderStatus.PAID, OrderStatus.CANCELLED));
        VALID_TRANSITIONS.put(OrderStatus.CANCELLED, Set.of());
    }

    public void validateTransition(OrderStatus currentStatus, OrderStatus targetStatus, OrderType type) {
        Set<OrderStatus> allowed = VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of());

        if (!allowed.contains(targetStatus)) {
            throw new IllegalStateException(String.format("Luồng trạng thái không hợp lệ: Không thể chuyển từ %s sang %s", currentStatus, targetStatus));
        }

        // Validate nghiệp vụ riêng theo loại đơn
        if (type == OrderType.DINE_IN && (targetStatus == OrderStatus.PREPARING || targetStatus == OrderStatus.OUT_FOR_DELIVERY)) {
            throw new IllegalStateException("Đơn tại bàn không được sử dụng trạng thái giao hàng.");
        }
        if (type == OrderType.DELIVERY && targetStatus == OrderStatus.COOKING) {
            throw new IllegalStateException("Đơn giao hàng phải dùng trạng thái PREPARING thay vì COOKING.");
        }
    }
}