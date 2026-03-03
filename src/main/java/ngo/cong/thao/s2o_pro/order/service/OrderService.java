package ngo.cong.thao.s2o_pro.order.service;

import ngo.cong.thao.s2o_pro.order.dto.OrderRequest;
import ngo.cong.thao.s2o_pro.order.entity.Order;
import ngo.cong.thao.s2o_pro.order.entity.OrderStatus;

import java.util.UUID;

public interface OrderService {
    Order createOrder(OrderRequest request);
    Order updateOrderStatus(UUID orderId, OrderStatus newStatus);
}