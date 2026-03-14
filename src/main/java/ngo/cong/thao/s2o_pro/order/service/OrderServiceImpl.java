package ngo.cong.thao.s2o_pro.order.service;

import lombok.RequiredArgsConstructor;
import ngo.cong.thao.s2o_pro.menu.entity.MenuItem;
import ngo.cong.thao.s2o_pro.menu.repository.MenuItemRepository;
import ngo.cong.thao.s2o_pro.order.dto.OrderRequest;
import ngo.cong.thao.s2o_pro.order.dto.OrderResponse;
import ngo.cong.thao.s2o_pro.order.entity.*;
import ngo.cong.thao.s2o_pro.order.event.OrderPaidEvent;
import ngo.cong.thao.s2o_pro.order.repository.OrderRepository;
import ngo.cong.thao.s2o_pro.tenant.TenantContext;
import ngo.cong.thao.s2o_pro.common.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderStateEngine stateEngine;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    // THÊM: Inject NotificationService để bắn thông báo Real-time
    private final NotificationService notificationService;

    @Override
    @Transactional
    public Order createOrder(OrderRequest request) {
        // 1. Validate cơ bản
        if (request.getOrderType() == OrderType.DINE_IN && (request.getTableId() == null || request.getTableId().isEmpty())) {
            throw new IllegalArgumentException("Đơn dùng tại quán bắt buộc phải có ID Bàn (tableId)");
        }
        if (request.getOrderType() == OrderType.DELIVERY && (request.getDeliveryAddress() == null || request.getDeliveryAddress().isEmpty())) {
            throw new IllegalArgumentException("Đơn giao hàng bắt buộc phải có Địa chỉ (deliveryAddress)");
        }

        Order order;
        boolean isAddingToExisting = false;

        // 2. Logic GỘP BILL cho khách ăn tại bàn (DINE_IN)
        if (request.getOrderType() == OrderType.DINE_IN) {
            java.util.Optional<Order> activeOrderOpt = orderRepository.findActiveOrderByTableId(request.getTableId());

            if (activeOrderOpt.isPresent()) {
                order = activeOrderOpt.get();
                isAddingToExisting = true;

                // Nếu bếp đã làm xong hết món cũ (DONE), giờ có món mới -> Bật lại trạng thái COOKING
                if (order.getStatus() == OrderStatus.DONE) {
                    order.setStatus(OrderStatus.COOKING);
                }
            } else {
                // Bàn trống, tạo Bill mới
                order = Order.builder()
                        .orderType(request.getOrderType())
                        .status(OrderStatus.NEW)
                        .tableId(request.getTableId())
                        .totalAmount(BigDecimal.ZERO)
                        .build();
                order.setTenantId(TenantContext.getTenantId());
            }
        } else {
            // Đơn DELIVERY luôn tạo mới
            order = Order.builder()
                    .orderType(request.getOrderType())
                    .status(OrderStatus.PENDING_PAYMENT)
                    .customerName(request.getCustomerName())
                    .customerPhone(request.getCustomerPhone())
                    .deliveryAddress(request.getDeliveryAddress())
                    .deliveryNotes(request.getDeliveryNotes())
                    .totalAmount(BigDecimal.ZERO)
                    .build();
            order.setTenantId(TenantContext.getTenantId());
        }

        // 3. Quét danh sách món gọi thêm / gọi mới
        BigDecimal currentTotal = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;

        for (var itemReq : request.getItems()) {
            MenuItem menuItem = menuItemRepository.findById(itemReq.getMenuItemId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy món ăn: " + itemReq.getMenuItemId()));

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .menuItem(menuItem)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(menuItem.getPrice())
                    .notes(itemReq.getNotes())
                    .build();
            orderItem.setTenantId(TenantContext.getTenantId());

            order.getItems().add(orderItem);

            // Cộng dồn tiền vào Bill
            BigDecimal itemTotal = menuItem.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            currentTotal = currentTotal.add(itemTotal);
        }

        order.setTotalAmount(currentTotal);
        Order savedOrder = orderRepository.save(order);

        // 4. CHỈ bắn thông báo cho Bếp nếu đơn hàng là NEW (Ăn tại bàn).
        // Nếu là giao hàng (PENDING_PAYMENT) thì im lặng, chờ thanh toán xong mới báo.
        if (savedOrder.getStatus() == OrderStatus.NEW) {
            String tableName = (request.getTableId() != null && !request.getTableId().isEmpty())
                    ? request.getTableId() : "Giao hàng/Mang đi";
            String notiMessage = isAddingToExisting
                    ? "🔔 Bàn " + tableName + " VỪA GỌI THÊM MÓN!"
                    : "🔔 Ting Ting! Bàn " + tableName + " vừa gọi món mới!";
            notificationService.notifyKitchenNewOrder(savedOrder.getTenantId(), tableName, savedOrder.getId().toString());
        }



        // 5. Kích hoạt Event báo rằng có đơn hàng mới được tạo (để hệ thống tự đi khoá bàn)
        if (!isAddingToExisting && request.getOrderType() == OrderType.DINE_IN) {
            eventPublisher.publishEvent(new ngo.cong.thao.s2o_pro.order.event.OrderCreatedEvent(request.getTableId(), savedOrder.getTenantId()));
        }

        return savedOrder;
    } // Hết hàm createOrder // Bỏ qua lỗi ép kiểu


    @Override
    @Transactional
    public Order updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        // 1. Dùng State Engine để kiểm tra logic
        stateEngine.validateTransition(order.getStatus(), newStatus, order.getOrderType());

        // 2. Lưu trạng thái mới
        order.setStatus(newStatus);
        Order savedOrder = orderRepository.save(order);

        // 3. Nếu khách thanh toán xong -> Bắn pháo hoa (Event)
        if (newStatus == OrderStatus.PAID) {
            eventPublisher.publishEvent(new OrderPaidEvent(
                    savedOrder.getId(),
                    savedOrder.getTotalAmount(),
                    savedOrder.getTenantId(),
                    savedOrder.getTableId()

            ));
            String tableName = (savedOrder.getTableId() != null && !savedOrder.getTableId().isEmpty())
                    ? savedOrder.getTableId() : "Giao hàng/Mang đi";
            notificationService.notifyOrderStatusChanged(savedOrder.getTenantId(), savedOrder.getId().toString(), "PAID", tableName);
        }else if (newStatus == OrderStatus.READY && savedOrder.getOrderType() == OrderType.DELIVERY) {
            // Bắn sự kiện: Bếp nấu xong đơn Giao hàng -> Tự động tạo Vận đơn
            eventPublisher.publishEvent(new ngo.cong.thao.s2o_pro.order.event.OrderReadyForDeliveryEvent(
                    savedOrder.getId(),
                    savedOrder.getTenantId()
            ));

            // Vẫn giữ thông báo WebSocket cho Thu ngân biết để gọi Shipper
            notificationService.notifyOrderStatusChanged(savedOrder.getTenantId(), savedOrder.getId().toString(), "READY", "Giao hàng");

    } else {
        // --- XỬ LÝ ĐẶC BIỆT KHI ĐƠN ONLINE THANH TOÁN XONG (PENDING -> NEW) ---
        if (newStatus == OrderStatus.NEW && savedOrder.getOrderType() == OrderType.DELIVERY) {
            // Tiền đã vào tài khoản, bây giờ mới hú Bếp làm món!
            notificationService.notifyKitchenNewOrder(savedOrder.getTenantId(), "Giao hàng Online", savedOrder.getId().toString());
        }

        // Bắn thông báo Real-time cho Thu ngân/Phục vụ
        String tableName = (savedOrder.getTableId() != null && !savedOrder.getTableId().isEmpty())
                ? savedOrder.getTableId()
                : "Giao hàng/Mang đi";
        notificationService.notifyOrderStatusChanged(
                savedOrder.getTenantId(),
                savedOrder.getId().toString(),
                newStatus.name(),
                tableName
        );
    }

        return savedOrder;
    }
    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Order> getOrdersByStatuses(java.util.List<OrderStatus> statuses, org.springframework.data.domain.Pageable pageable) {
        // Nếu không truyền status nào thì lấy tất cả
        if (statuses == null || statuses.isEmpty()) {
            return orderRepository.findAll(pageable);
        }
        return orderRepository.findAllByStatusInOrderByCreatedAtAsc(statuses, pageable);
    }
    @Override
    @Transactional
    public Order payOrder(UUID orderId, ngo.cong.thao.s2o_pro.order.dto.OrderPayRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        // 1. Dùng State Engine kiểm tra xem đơn này có được phép thanh toán không (Phải đang DONE hoặc DELIVERED)
        stateEngine.validateTransition(order.getStatus(), OrderStatus.PAID, order.getOrderType());

        // 2. Tính toán tiền thừa nếu thanh toán bằng Tiền mặt
        BigDecimal amountGiven = request.getAmountGiven();
        BigDecimal changeAmount = BigDecimal.ZERO;

        if (request.getPaymentMethod() == ngo.cong.thao.s2o_pro.order.entity.PaymentMethod.CASH) {
            if (amountGiven == null || amountGiven.compareTo(order.getTotalAmount()) < 0) {
                throw new IllegalArgumentException("Số tiền khách đưa không đủ để thanh toán!");
            }
            changeAmount = amountGiven.subtract(order.getTotalAmount());
        } else {
            // Chuyển khoản hoặc Quẹt thẻ thì mặc định coi như đưa vừa đủ
            amountGiven = order.getTotalAmount();
        }

        // 3. Cập nhật dữ liệu
        order.setStatus(OrderStatus.PAID);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setAmountGiven(amountGiven);
        order.setChangeAmount(changeAmount);

        Order savedOrder = orderRepository.save(order);

        // 4. Kích hoạt Event bắn pháo hoa
        eventPublisher.publishEvent(new ngo.cong.thao.s2o_pro.order.event.OrderPaidEvent(
                savedOrder.getId(),
                savedOrder.getTotalAmount(),
                savedOrder.getTenantId(),
                savedOrder.getTableId()
        ));

        // Bắn thông báo về Bếp để giải phóng màn hình (Tùy chọn)
        notificationService.notifyOrderStatusChanged(savedOrder.getTenantId(), savedOrder.getId().toString(), "PAID", savedOrder.getTableId() != null ? savedOrder.getTableId() : "Giao hàng");

        return savedOrder;
    }

    @Override
    @Transactional(readOnly = true)
    public ngo.cong.thao.s2o_pro.order.dto.DashboardSummaryResponse getDashboardSummary(java.time.LocalDate startDate, java.time.LocalDate endDate) {

        // 1. Xử lý thời gian (Nếu null thì mặc định lấy ngày hôm nay)
        java.time.LocalDateTime start = (startDate != null) ? startDate.atStartOfDay() : java.time.LocalDate.now().atStartOfDay();
        java.time.LocalDateTime end = (endDate != null) ? endDate.atTime(java.time.LocalTime.MAX) : java.time.LocalDate.now().atTime(java.time.LocalTime.MAX);

        // 2. Gọi các hàm thống kê
        long totalOrders = orderRepository.countByStatusAndCreatedAtBetween(OrderStatus.PAID, start, end);

        BigDecimal revenue = orderRepository.sumRevenue(OrderStatus.PAID, start, end);
        BigDecimal dineInRev = orderRepository.sumRevenueByType(OrderStatus.PAID, OrderType.DINE_IN, start, end);
        BigDecimal deliveryRev = orderRepository.sumRevenueByType(OrderStatus.PAID, OrderType.DELIVERY, start, end);

        // Lấy Top 5 món bán chạy nhất
        java.util.List<ngo.cong.thao.s2o_pro.order.dto.ItemSalesDto> topItems = orderRepository.getTopSellingItems(
                OrderStatus.PAID, start, end, org.springframework.data.domain.PageRequest.of(0, 5)
        );

        // 3. Đóng gói kết quả
        return ngo.cong.thao.s2o_pro.order.dto.DashboardSummaryResponse.builder()
                .totalRevenue(revenue != null ? revenue : BigDecimal.ZERO)
                .totalOrders(totalOrders)
                .dineInRevenue(dineInRev != null ? dineInRev : BigDecimal.ZERO)
                .deliveryRevenue(deliveryRev != null ? deliveryRev : BigDecimal.ZERO)
                .topSellingItems(topItems)
                .build();
    }
    @Override
    @Transactional
    public ngo.cong.thao.s2o_pro.order.dto.OrderResponse callForPayment(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        // ---> BỔ SUNG CHỐT CHẶN Ở ĐÂY: Chỉ cho phép đơn DINE_IN <---
        if (order.getOrderType() != ngo.cong.thao.s2o_pro.order.entity.OrderType.DINE_IN) {
            throw new IllegalArgumentException("Tính năng gọi thanh toán chỉ áp dụng cho đơn ăn tại bàn!");
        }

        // 1. Gọi hàm cập nhật trạng thái
        Order updatedOrder = updateOrderStatus(orderId, OrderStatus.PAYMENT_REQUESTED);

        // 2. Chuyển đổi từ Order sang OrderResponse
        return ngo.cong.thao.s2o_pro.order.dto.OrderResponse.fromEntity(updatedOrder);
    }
}