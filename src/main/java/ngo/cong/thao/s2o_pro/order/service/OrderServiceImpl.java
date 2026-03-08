package ngo.cong.thao.s2o_pro.order.service;

import lombok.RequiredArgsConstructor;
import ngo.cong.thao.s2o_pro.menu.entity.MenuItem;
import ngo.cong.thao.s2o_pro.menu.repository.MenuItemRepository;
import ngo.cong.thao.s2o_pro.order.dto.OrderRequest;
import ngo.cong.thao.s2o_pro.order.entity.Order;
import ngo.cong.thao.s2o_pro.order.entity.OrderItem;
import ngo.cong.thao.s2o_pro.order.entity.OrderStatus;
import ngo.cong.thao.s2o_pro.order.entity.OrderType;
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
                    .status(OrderStatus.NEW)
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

        // 4. Bắn thông báo cho Bếp
        String tableName = (request.getTableId() != null && !request.getTableId().isEmpty())
                ? request.getTableId() : (request.getOrderType() == OrderType.DELIVERY ? "Giao hàng" : "Mang đi");

        String notiMessage = isAddingToExisting
                ? "🔔 Bàn " + tableName + " VỪA GỌI THÊM MÓN!"
                : "🔔 Ting Ting! Bàn " + tableName + " vừa gọi món mới!";

        // Tùy biến xíu hàm notifyKitchen (Truyền thêm message) - Anh có thể cập nhật trong NotificationService nếu muốn
        notificationService.notifyKitchenNewOrder(savedOrder.getTenantId(), tableName, savedOrder.getId().toString());

        // 5. Nếu là Bill mới, chốt sổ cho Bàn thành "Có khách" (OCCUPIED) luôn cho chắc cú
        if (!isAddingToExisting && request.getOrderType() == OrderType.DINE_IN) {
            try {
                java.util.UUID tableUuid = java.util.UUID.fromString(request.getTableId());
                ngo.cong.thao.s2o_pro.table.repository.DiningTableRepository tableRepo =
                        org.springframework.web.context.support.WebApplicationContextUtils
                                .getRequiredWebApplicationContext(
                                        ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes()).getRequest().getServletContext()
                                ).getBean(ngo.cong.thao.s2o_pro.table.repository.DiningTableRepository.class);

                tableRepo.findById(tableUuid).ifPresent(t -> {
                    t.setStatus(ngo.cong.thao.s2o_pro.table.entity.DiningTable.TableStatus.OCCUPIED);
                    tableRepo.save(t);
                });
            } catch (Exception ignored) {} // Bỏ qua lỗi ép kiểu
        }

        return savedOrder;
    }

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
        }
        else {
            // --- THÊM MỚI TẠI ĐÂY: Bắn thông báo Real-time cho Thu ngân/Phục vụ ---
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
}