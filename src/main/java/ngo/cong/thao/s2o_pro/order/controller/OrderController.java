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
    @PreAuthorize("hasAnyRole('OWNER', 'CASHIER', 'CUSTOMER', 'GUEST')")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody OrderRequest request) {
        Order savedOrder = orderService.createOrder(request);
        return ResponseEntity.ok(ApiResponse.success(OrderResponse.fromEntity(savedOrder)));
    }

    // CẬP NHẬT TRẠNG THÁI (Test State Machine)
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('OWNER', 'CASHIER', 'CHEF')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable UUID id,
            @RequestParam OrderStatus status) {

        Order updatedOrder = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(OrderResponse.fromEntity(updatedOrder)));
    }

    // LẤY DANH SÁCH ĐƠN HÀNG THEO TRẠNG THÁI (Dùng cho Màn hình Bếp / Thu ngân)
    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'CASHIER', 'CHEF')")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<OrderResponse>>> getOrders(
            @RequestParam(required = false) java.util.List<OrderStatus> statuses,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        org.springframework.data.domain.Page<Order> orderPage = orderService.getOrdersByStatuses(
                statuses,
                org.springframework.data.domain.PageRequest.of(page, size)
        );

        // Map từ Entity sang DTO
        org.springframework.data.domain.Page<OrderResponse> responsePage = orderPage.map(OrderResponse::fromEntity);

        return ResponseEntity.ok(ApiResponse.success(responsePage));
    }

    // THANH TOÁN ĐƠN HÀNG (Dành riêng cho Thu ngân)
    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('OWNER', 'CASHIER')")
    public ResponseEntity<ApiResponse<OrderResponse>> payOrder(
            @PathVariable UUID id,
            @Valid @RequestBody ngo.cong.thao.s2o_pro.order.dto.OrderPayRequest request) {

        Order paidOrder = orderService.payOrder(id, request);
        return ResponseEntity.ok(ApiResponse.success(OrderResponse.fromEntity(paidOrder)));
    }

    // API LẤY THỐNG KÊ DOANH THU (Có thể truyền thêm ?startDate=2024-01-01&endDate=2024-01-31)
    @GetMapping("/dashboard/summary")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<ngo.cong.thao.s2o_pro.order.dto.DashboardSummaryResponse>> getDashboardSummary(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate) {

        ngo.cong.thao.s2o_pro.order.dto.DashboardSummaryResponse summary = orderService.getDashboardSummary(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}