package ngo.cong.thao.s2o_pro.order.service;

import ngo.cong.thao.s2o_pro.order.dto.OrderRequest;
import ngo.cong.thao.s2o_pro.order.entity.Order;
import ngo.cong.thao.s2o_pro.order.entity.OrderStatus;

import java.util.UUID;

public interface OrderService {
    Order createOrder(OrderRequest request);
    Order updateOrderStatus(UUID orderId, OrderStatus newStatus);
    // Thêm vào interface
    Order payOrder(UUID orderId, ngo.cong.thao.s2o_pro.order.dto.OrderPayRequest request);
    org.springframework.data.domain.Page<Order> getOrdersByStatuses(java.util.List<OrderStatus> statuses, org.springframework.data.domain.Pageable pageable);
}