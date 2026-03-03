package ngo.cong.thao.s2o_pro.order.entity;

import jakarta.persistence.*;
import lombok.*;
import ngo.cong.thao.s2o_pro.common.entity.TenantAwareEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_tenant", columnList = "tenant_id"),
        @Index(name = "idx_order_status", columnList = "status"),
        @Index(name = "idx_order_tenant_status", columnList = "tenant_id, status") // Index gộp cho query phổ biến nhất
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends TenantAwareEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    // --- Dành cho DINE_IN ---
    @Column(name = "table_id")
    private String tableId;

    // --- Dành cho DELIVERY ---
    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "delivery_address")
    private String deliveryAddress;

    @Column(name = "delivery_notes")
    private String deliveryNotes;

    // Liên kết với chi tiết đơn hàng (1 đơn có nhiều món)
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();
}