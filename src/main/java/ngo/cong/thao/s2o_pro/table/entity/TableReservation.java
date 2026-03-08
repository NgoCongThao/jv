package ngo.cong.thao.s2o_pro.table.entity;

import jakarta.persistence.*;
import lombok.*;
import ngo.cong.thao.s2o_pro.common.entity.TenantAwareEntity;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "table_reservations", indexes = {
        @Index(name = "idx_reservation_tenant", columnList = "tenant_id"),
        @Index(name = "idx_reservation_time", columnList = "reservation_time")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableReservation extends TenantAwareEntity {

    @Column(name = "table_id", nullable = false)
    private UUID tableId;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_phone", nullable = false)
    private String customerPhone;

    // Thời gian khách dự kiến đến
    @Column(name = "reservation_time", nullable = false)
    private LocalDateTime reservationTime;

    // Số lượng khách dự kiến
    @Column(name = "guest_count")
    private int guestCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.CONFIRMED;

    public enum ReservationStatus {
        CONFIRMED,  // Đã chốt đặt
        COMPLETED,  // Khách đã đến và nhận bàn
        CANCELLED   // Khách hủy
    }
}