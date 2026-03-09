package ngo.cong.thao.s2o_pro.delivery.entity;

import jakarta.persistence.*;
import lombok.*;
import ngo.cong.thao.s2o_pro.common.entity.TenantAwareEntity;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "delivery_tickets", indexes = {
        @Index(name = "idx_delivery_tenant", columnList = "tenant_id"),
        @Index(name = "idx_delivery_order", columnList = "order_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryTicket extends TenantAwareEntity {

    @Column(name = "order_id", nullable = false, unique = true) // 1 Đơn hàng chỉ có 1 Vận đơn
    private UUID orderId;

    // Thông tin Shipper (Có thể null lúc mới tạo vì chưa tìm được tài xế)
    @Column(name = "shipper_name")
    private String shipperName;

    @Column(name = "shipper_phone")
    private String shipperPhone;

    @Column(name = "license_plate")
    private String licensePlate;

    @Column(name = "delivery_fee")
    @Builder.Default
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TicketStatus status = TicketStatus.FINDING_SHIPPER;

    public enum TicketStatus {
        FINDING_SHIPPER, // Đang tìm tài xế
        PICKING_UP,      // Tài xế đang đến lấy hàng
        ON_THE_WAY,      // Đang giao cho khách
        DELIVERED,       // Đã giao thành công
        CANCELLED        // Hủy giao
    }
}