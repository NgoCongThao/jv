package ngo.cong.thao.s2o_pro.order.service;

import ngo.cong.thao.s2o_pro.order.dto.OrderRequest;
import ngo.cong.thao.s2o_pro.order.dto.OrderResponse;
import ngo.cong.thao.s2o_pro.order.entity.Order;
import ngo.cong.thao.s2o_pro.order.entity.OrderStatus;

import java.util.UUID;

public interface OrderService {
    Order createOrder(OrderRequest request);
    Order updateOrderStatus(UUID orderId, OrderStatus newStatus);
    // Thêm vào interface
    Order payOrder(UUID orderId, ngo.cong.thao.s2o_pro.order.dto.OrderPayRequest request);
    org.springframework.data.domain.Page<Order> getOrdersByStatuses(java.util.List<OrderStatus> statuses, org.springframework.data.domain.Pageable pageable);
    ngo.cong.thao.s2o_pro.order.dto.DashboardSummaryResponse getDashboardSummary(java.time.LocalDate startDate, java.time.LocalDate endDate);

    // Khách hàng bấm nút gọi thanh toán (Không cần chọn phương thức)
    OrderResponse callForPayment(UUID orderId);
}