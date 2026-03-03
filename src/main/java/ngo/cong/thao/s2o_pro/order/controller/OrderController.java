package ngo.cong.thao.s2o_pro.order.controller;

import jakarta.validation.Valid;
import ngo.cong.thao.s2o_pro.common.response.ApiResponse;
import ngo.cong.thao.s2o_pro.order.dto.OrderRequest;
import ngo.cong.thao.s2o_pro.order.dto.OrderResponse;
import ngo.cong.thao.s2o_pro.order.entity.Order;
import ngo.cong.thao.s2o_pro.order.entity.OrderStatus;
import ngo.cong.thao.s2o_pro.order.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // TẠO ĐƠN HÀNG MỚI
    @PostMapping
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'CASHIER', 'CUSTOMER', 'GUEST')")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody OrderRequest request) {
        Order savedOrder = orderService.createOrder(request);
        return ResponseEntity.ok(ApiResponse.success(OrderResponse.fromEntity(savedOrder)));
    }

    // CẬP NHẬT TRẠNG THÁI (Test State Machine)
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'CASHIER', 'CHEF')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable UUID id,
            @RequestParam OrderStatus status) {

        Order updatedOrder = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(OrderResponse.fromEntity(updatedOrder)));
    }
}