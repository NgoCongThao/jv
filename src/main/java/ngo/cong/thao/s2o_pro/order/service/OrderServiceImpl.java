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

@lombok.extern.slf4j.Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderStateEngine stateEngine;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    private final ngo.cong.thao.s2o_pro.user.repository.CustomerMembershipRepository membershipRepository;
    // ĐÃ FIX: Bổ sung UserRepository bị thiếu
    private final ngo.cong.thao.s2o_pro.user.repository.UserRepository userRepository;

    private final NotificationService notificationService;

    @Override
    @Transactional
    public Order createOrder(OrderRequest request) {

        java.util.UUID loggedInCustomerId = null;
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        // Bỏ check instanceof UserDetails, chỉ cần check đã login và không phải khách vãng lai
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String phone = auth.getName(); // Lấy đúng số điện thoại từ Token
            java.util.Optional<ngo.cong.thao.s2o_pro.user.entity.User> userOpt = userRepository.findByUsername(phone);

            if (userOpt.isPresent()) {
                ngo.cong.thao.s2o_pro.user.entity.User u = userOpt.get();
                loggedInCustomerId = u.getId();

                // NẾU KHÁCH LẦN ĐẦU TỚI QUÁN -> PHÁT THẺ THÀNH VIÊN VÍ RỖNG CỦA QUÁN NÀY
                String currentTenant = ngo.cong.thao.s2o_pro.tenant.TenantContext.getTenantId();
                if (currentTenant != null && membershipRepository.findByCustomerIdAndTenantId(u.getId(), currentTenant).isEmpty()) {
                    membershipRepository.save(ngo.cong.thao.s2o_pro.user.entity.CustomerMembership.builder()
                            .customerId(u.getId())
                            .tenantId(currentTenant)
                            .points(0)
                            .totalSpent(java.math.BigDecimal.ZERO)
                            .build());
                }
            }
        }
        // 2. Validate cơ bản
        if (request.getOrderType() == OrderType.DINE_IN && (request.getTableId() == null || request.getTableId().isEmpty())) {
            throw new IllegalArgumentException("Đơn dùng tại quán bắt buộc phải có ID Bàn (tableId)");
        }
        if (request.getOrderType() == OrderType.DELIVERY && (request.getDeliveryAddress() == null || request.getDeliveryAddress().isEmpty())) {
            throw new IllegalArgumentException("Đơn giao hàng bắt buộc phải có Địa chỉ (deliveryAddress)");
        }

        Order order = new Order();
        boolean isAddingToExisting = false;

        // 3. Logic GỘP BILL cho khách ăn tại bàn (DINE_IN)
        if (request.getOrderType() == OrderType.DINE_IN) {
            java.util.Optional<Order> activeOrderOpt = orderRepository.findActiveOrderByTableId(request.getTableId());

            if (activeOrderOpt.isPresent()) {
                Order existingOrder = activeOrderOpt.get();
                order = existingOrder;
                isAddingToExisting = true;

                if (order.getStatus() == OrderStatus.DONE) {
                    order.setStatus(OrderStatus.COOKING);
                }
            } else {
                order.setOrderType(request.getOrderType());
                order.setStatus(OrderStatus.NEW);
                order.setTableId(request.getTableId());
                order.setTotalAmount(BigDecimal.ZERO);
                order.setTenantId(TenantContext.getTenantId());
            }
        } else {
            order.setOrderType(request.getOrderType());
            order.setStatus(OrderStatus.PENDING_PAYMENT);
            order.setCustomerName(request.getCustomerName());
            order.setCustomerPhone(request.getCustomerPhone());
            order.setDeliveryAddress(request.getDeliveryAddress());
            order.setDeliveryNotes(request.getDeliveryNotes());
            order.setTotalAmount(BigDecimal.ZERO);
            order.setTenantId(TenantContext.getTenantId());
        }

        // ---> GẮN ID KHÁCH HÀNG VÀO ĐƠN SAU KHI ĐÃ XỬ LÝ XONG <---
        if (loggedInCustomerId != null) {
            order.setCustomerId(loggedInCustomerId);
        }

        // 4. Quét danh sách món gọi thêm / gọi mới
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
            BigDecimal itemTotal = menuItem.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            currentTotal = currentTotal.add(itemTotal);
        }

        order.setTotalAmount(currentTotal);
        Order savedOrder = orderRepository.save(order);

        // 5. Bắn thông báo cho Bếp
        if (savedOrder.getStatus() == OrderStatus.NEW) {
            String tableName = (request.getTableId() != null && !request.getTableId().isEmpty())
                    ? request.getTableId() : "Giao hàng/Mang đi";
            notificationService.notifyKitchenNewOrder(savedOrder.getTenantId(), tableName, savedOrder.getId().toString());
        }

        // 6. Khoá bàn
        if (!isAddingToExisting && request.getOrderType() == OrderType.DINE_IN) {
            eventPublisher.publishEvent(new ngo.cong.thao.s2o_pro.order.event.OrderCreatedEvent(request.getTableId(), savedOrder.getTenantId()));
        }

        return savedOrder;
    }


    @Override
    @Transactional
    public Order updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        stateEngine.validateTransition(order.getStatus(), newStatus, order.getOrderType());

        order.setStatus(newStatus);
        Order savedOrder = orderRepository.save(order);

        if (newStatus == OrderStatus.PAID) {
            eventPublisher.publishEvent(new OrderPaidEvent(savedOrder.getId(), savedOrder.getTotalAmount(), savedOrder.getTenantId(), savedOrder.getTableId()));

            String tableName = (savedOrder.getTableId() != null && !savedOrder.getTableId().isEmpty()) ? savedOrder.getTableId() : "Giao hàng/Mang đi";
            notificationService.notifyOrderStatusChanged(savedOrder.getTenantId(), savedOrder.getId().toString(), "PAID", tableName);

            // ĐÃ FIX: Di chuyển TÍCH ĐIỂM vào đúng chỗ khi ĐÃ THANH TOÁN (PAID)
            if (savedOrder.getCustomerId() != null) {
                membershipRepository.findByCustomerIdAndTenantId(savedOrder.getCustomerId(), savedOrder.getTenantId())
                        .ifPresent(membership -> {
                            int pointsEarned = savedOrder.getTotalAmount().intValue() / 10000;
                            membership.setPoints(membership.getPoints() + pointsEarned);
                            membership.setTotalSpent(membership.getTotalSpent().add(savedOrder.getTotalAmount()));
                            membershipRepository.save(membership);
                            log.info("🎁 Khách {} đã được cộng {} điểm vào ví của nhà hàng {}", savedOrder.getCustomerName(), pointsEarned, savedOrder.getTenantId());
                        });
            }

        } else if (newStatus == OrderStatus.READY && savedOrder.getOrderType() == OrderType.DELIVERY) {
            eventPublisher.publishEvent(new ngo.cong.thao.s2o_pro.order.event.OrderReadyForDeliveryEvent(savedOrder.getId(), savedOrder.getTenantId()));
            notificationService.notifyOrderStatusChanged(savedOrder.getTenantId(), savedOrder.getId().toString(), "READY", "Giao hàng");

        } else {
            if (newStatus == OrderStatus.NEW && savedOrder.getOrderType() == OrderType.DELIVERY) {
                notificationService.notifyKitchenNewOrder(savedOrder.getTenantId(), "Giao hàng Online", savedOrder.getId().toString());
            }

            String tableName = (savedOrder.getTableId() != null && !savedOrder.getTableId().isEmpty()) ? savedOrder.getTableId() : "Giao hàng/Mang đi";
            notificationService.notifyOrderStatusChanged(savedOrder.getTenantId(), savedOrder.getId().toString(), newStatus.name(), tableName);
        }

        return savedOrder;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Order> getOrdersByStatuses(java.util.List<OrderStatus> statuses, org.springframework.data.domain.Pageable pageable) {
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

        stateEngine.validateTransition(order.getStatus(), OrderStatus.PAID, order.getOrderType());

        BigDecimal finalTotal = order.getTotalAmount();
        Integer pointsToUse = request.getPointsToUse() != null ? request.getPointsToUse() : 0;
        BigDecimal discountAmount = BigDecimal.ZERO;

        // 1. LOGIC XÀI ĐIỂM ĐỔI TIỀN (BURNING POINTS)
        if (pointsToUse > 0) {
            if (order.getCustomerId() == null) {
                throw new IllegalArgumentException("Đơn này là của khách vãng lai, không thể dùng điểm!");
            }

            ngo.cong.thao.s2o_pro.user.entity.CustomerMembership membership = membershipRepository
                    .findByCustomerIdAndTenantId(order.getCustomerId(), order.getTenantId())
                    .orElseThrow(() -> new IllegalArgumentException("Khách chưa có thẻ thành viên tại quán này!"));

            if (membership.getPoints() < pointsToUse) {
                throw new IllegalArgumentException("Không đủ điểm! Ví của khách chỉ có " + membership.getPoints() + " điểm.");
            }

            // TỶ LỆ QUY ĐỔI: Quán tự cấu hình (Tạm fix 1 Điểm = 1.000 VNĐ)
            BigDecimal pointValue = new BigDecimal("1000");
            discountAmount = pointValue.multiply(new BigDecimal(pointsToUse));

            // Chống lỗi "Giảm giá lố tiền Bill"
            if (discountAmount.compareTo(finalTotal) > 0) {
                discountAmount = finalTotal;
                // Tính lại số điểm thực tế bị trừ
                pointsToUse = finalTotal.divide(pointValue, java.math.RoundingMode.UP).intValue();
            }

            finalTotal = finalTotal.subtract(discountAmount); // Cập nhật lại số tiền khách phải trả

            // Trừ điểm trong ví ngay lập tức
            membership.setPoints(membership.getPoints() - pointsToUse);
            membershipRepository.save(membership);

            // Ghi vết vào Đơn hàng
            order.setPointsUsed(pointsToUse);
            order.setDiscountAmount(discountAmount);
            log.info("💳 Khách hàng đã dùng {} điểm để giảm {} VNĐ", pointsToUse, discountAmount);
        }

        // 2. TÍNH TIỀN THỪA (Dựa trên finalTotal đã giảm giá)
        BigDecimal amountGiven = request.getAmountGiven();
        BigDecimal changeAmount = BigDecimal.ZERO;

        if (request.getPaymentMethod() == ngo.cong.thao.s2o_pro.order.entity.PaymentMethod.CASH) {
            if (amountGiven == null || amountGiven.compareTo(finalTotal) < 0) {
                throw new IllegalArgumentException("Số tiền khách đưa không đủ để thanh toán (" + finalTotal + " VNĐ)!");
            }
            changeAmount = amountGiven.subtract(finalTotal);
        } else {
            amountGiven = finalTotal;
        }

        order.setStatus(OrderStatus.PAID);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setAmountGiven(amountGiven);
        order.setChangeAmount(changeAmount);

        Order savedOrder = orderRepository.save(order);

        eventPublisher.publishEvent(new ngo.cong.thao.s2o_pro.order.event.OrderPaidEvent(savedOrder.getId(), savedOrder.getTotalAmount(), savedOrder.getTenantId(), savedOrder.getTableId()));
        notificationService.notifyOrderStatusChanged(savedOrder.getTenantId(), savedOrder.getId().toString(), "PAID", savedOrder.getTableId() != null ? savedOrder.getTableId() : "Giao hàng");

        // 3. LOGIC TÍCH ĐIỂM MỚI (CHỈ TÍNH TRÊN SỐ TIỀN THỰC TRẢ - finalTotal)
        if (savedOrder.getCustomerId() != null && finalTotal.compareTo(BigDecimal.ZERO) > 0) {

            // Dùng Optional thuần túy, bỏ dùng Lambda để tránh lỗi effectively final
            java.util.Optional<ngo.cong.thao.s2o_pro.user.entity.CustomerMembership> memOpt =
                    membershipRepository.findByCustomerIdAndTenantId(savedOrder.getCustomerId(), savedOrder.getTenantId());

            if (memOpt.isPresent()) {
                ngo.cong.thao.s2o_pro.user.entity.CustomerMembership membership = memOpt.get();

                int pointsEarned = finalTotal.intValue() / 10000; // 10k = 1 điểm
                if (pointsEarned > 0) {
                    membership.setPoints(membership.getPoints() + pointsEarned);
                    membership.setTotalSpent(membership.getTotalSpent().add(finalTotal));
                    membershipRepository.save(membership);
                    log.info("🎁 Khách đã được cộng thêm {} điểm từ số tiền thực trả {} VNĐ", pointsEarned, finalTotal);
                }
            }
        }

        return savedOrder;
    }

    @Override
    @Transactional(readOnly = true)
    public ngo.cong.thao.s2o_pro.order.dto.DashboardSummaryResponse getDashboardSummary(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        java.time.LocalDateTime start = (startDate != null) ? startDate.atStartOfDay() : java.time.LocalDate.now().atStartOfDay();
        java.time.LocalDateTime end = (endDate != null) ? endDate.atTime(java.time.LocalTime.MAX) : java.time.LocalDate.now().atTime(java.time.LocalTime.MAX);

        long totalOrders = orderRepository.countByStatusAndCreatedAtBetween(OrderStatus.PAID, start, end);
        BigDecimal revenue = orderRepository.sumRevenue(OrderStatus.PAID, start, end);
        BigDecimal dineInRev = orderRepository.sumRevenueByType(OrderStatus.PAID, OrderType.DINE_IN, start, end);
        BigDecimal deliveryRev = orderRepository.sumRevenueByType(OrderStatus.PAID, OrderType.DELIVERY, start, end);

        java.util.List<ngo.cong.thao.s2o_pro.order.dto.ItemSalesDto> topItems = orderRepository.getTopSellingItems(
                OrderStatus.PAID, start, end, org.springframework.data.domain.PageRequest.of(0, 5)
        );

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

        if (order.getOrderType() != ngo.cong.thao.s2o_pro.order.entity.OrderType.DINE_IN) {
            throw new IllegalArgumentException("Tính năng gọi thanh toán chỉ áp dụng cho đơn ăn tại bàn!");
        }

        Order updatedOrder = updateOrderStatus(orderId, OrderStatus.PAYMENT_REQUESTED);
        return ngo.cong.thao.s2o_pro.order.dto.OrderResponse.fromEntity(updatedOrder);
    }
}