package ngo.cong.thao.s2o_pro.user.entity;

import jakarta.persistence.*;
import lombok.*;
import ngo.cong.thao.s2o_pro.common.entity.BaseEntity;

@Entity
@Table(name = "customer_memberships")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerMembership extends BaseEntity {

    @Column(name = "customer_id", nullable = false)
    private java.util.UUID customerId; // Trỏ về User (Khách hàng)

    @Column(name = "tenant_id", nullable = false)
    private String tenantId; // Ví điểm của nhà hàng nào (VD: "katinat")

    @Builder.Default
    @Column(nullable = false)
    private Integer points = 0; // Điểm tích lũy tại nhà hàng này

    @Builder.Default
    @Column(name = "total_spent", nullable = false)
    private java.math.BigDecimal totalSpent = java.math.BigDecimal.ZERO; // Tổng tiền đã chi tiêu để sau này tính Hạng (Vàng, Bạc...)
}