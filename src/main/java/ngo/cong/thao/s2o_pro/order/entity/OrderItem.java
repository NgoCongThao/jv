package ngo.cong.thao.s2o_pro.order.entity;

import jakarta.persistence.*;
import lombok.*;
import ngo.cong.thao.s2o_pro.common.entity.TenantAwareEntity;
import ngo.cong.thao.s2o_pro.menu.entity.MenuItem;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice; // Lưu lại giá tại thời điểm đặt, phòng khi sau này món ăn tăng giá

    private String notes; // Ghi chú riêng cho món (ví dụ: ít đá, nhiều hành)
}
