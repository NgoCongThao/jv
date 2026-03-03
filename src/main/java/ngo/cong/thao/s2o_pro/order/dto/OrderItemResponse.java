package ngo.cong.thao.s2o_pro.order.dto;

import lombok.Builder;
import lombok.Data;
import ngo.cong.thao.s2o_pro.order.entity.OrderItem;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class OrderItemResponse {
    private UUID id;
    private String menuItemName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String notes;

    public static OrderItemResponse fromEntity(OrderItem entity) {
        return OrderItemResponse.builder()
                .id(entity.getId())
                .menuItemName(entity.getMenuItem().getName())
                .quantity(entity.getQuantity())
                .unitPrice(entity.getUnitPrice())
                .totalPrice(entity.getUnitPrice().multiply(BigDecimal.valueOf(entity.getQuantity())))
                .notes(entity.getNotes())
                .build();
    }
}