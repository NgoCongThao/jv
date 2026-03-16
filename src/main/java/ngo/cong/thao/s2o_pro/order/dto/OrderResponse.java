package ngo.cong.thao.s2o_pro.order.dto;

import lombok.Builder;
import lombok.Data;
import ngo.cong.thao.s2o_pro.order.entity.Order;
import ngo.cong.thao.s2o_pro.order.entity.OrderStatus;
import ngo.cong.thao.s2o_pro.order.entity.OrderType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
public class OrderResponse {
    private UUID id;
    private OrderType orderType;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private Integer pointsUsed;
    private java.math.BigDecimal discountAmount;
    // DINE_IN
    private String tableId;

    // DELIVERY
    private String customerName;
    private String customerPhone;
    private String deliveryAddress;
    private String deliveryNotes;
    private String paymentMethod;
    private BigDecimal amountGiven;
    private BigDecimal changeAmount;
    private List<OrderItemResponse> items;

    public static OrderResponse fromEntity(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderType(order.getOrderType())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .tableId(order.getTableId())
                .customerName(order.getCustomerName())
                .customerPhone(order.getCustomerPhone())
                .deliveryAddress(order.getDeliveryAddress())
                .deliveryNotes(order.getDeliveryNotes())
                .items(order.getItems().stream()
                        .map(OrderItemResponse::fromEntity)
                        .collect(Collectors.toList()))
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)
                .amountGiven(order.getAmountGiven())
                .changeAmount(order.getChangeAmount())
                .pointsUsed(order.getPointsUsed())
                .discountAmount(order.getDiscountAmount())
                .build();
    }
}