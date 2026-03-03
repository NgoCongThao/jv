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
    @Override
    @Transactional
    public Order createOrder(OrderRequest request) {
        // 1. Validate linh hoạt theo loại đơn
        if (request.getOrderType() == OrderType.DINE_IN && (request.getTableId() == null || request.getTableId().isEmpty())) {
            throw new IllegalArgumentException("Đơn dùng tại quán bắt buộc phải có ID Bàn (tableId)");
        }
        if (request.getOrderType() == OrderType.DELIVERY && (request.getDeliveryAddress() == null || request.getDeliveryAddress().isEmpty())) {
            throw new IllegalArgumentException("Đơn giao hàng bắt buộc phải có Địa chỉ (deliveryAddress)");
        }

        // 2. Khởi tạo Order
        Order order = Order.builder()
                .orderType(request.getOrderType())
                .status(OrderStatus.NEW) // Luôn bắt đầu bằng NEW
                .tableId(request.getTableId())
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .deliveryAddress(request.getDeliveryAddress())
                .deliveryNotes(request.getDeliveryNotes())
                .totalAmount(BigDecimal.ZERO)
                .build();

        order.setTenantId(TenantContext.getTenantId());

        // 3. Xử lý danh sách món và tính tổng tiền
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (var itemReq : request.getItems()) {
            MenuItem menuItem = menuItemRepository.findById(itemReq.getMenuItemId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy món ăn: " + itemReq.getMenuItemId()));

            // Kiểm tra món có được bán trên kênh này không
            if (request.getOrderType() == OrderType.DINE_IN && !menuItem.isAvailableDineIn()) {
                throw new IllegalArgumentException("Món " + menuItem.getName() + " không phục vụ tại quán.");
            }
            if (request.getOrderType() == OrderType.DELIVERY && !menuItem.isAvailableDelivery()) {
                throw new IllegalArgumentException("Món " + menuItem.getName() + " không hỗ trợ giao hàng.");
            }

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .menuItem(menuItem)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(menuItem.getPrice()) // Chốt giá tại thời điểm đặt
                    .notes(itemReq.getNotes())
                    .build();
            orderItem.setTenantId(TenantContext.getTenantId());

            order.getItems().add(orderItem);

            // Tính tiền: (Giá * Số lượng) cộng dồn
            BigDecimal itemTotal = menuItem.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);
        }

        order.setTotalAmount(totalAmount);
        return orderRepository.save(order);
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
                    savedOrder.getTenantId()
            ));
        }

        return savedOrder;
    }
}