package ngo.cong.thao.s2o_pro.menu.entity;

import jakarta.persistence.*;
import lombok.*;
import ngo.cong.thao.s2o_pro.common.entity.TenantAwareEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;

@Entity
@Table(name = "menu_items", indexes = {
        @Index(name = "idx_menu_item_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItem extends TenantAwareEntity {

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "image_url")
    private String imageUrl;
    @JsonIgnore
    // Liên kết với Danh mục
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private MenuCategory category;

    // Phân loại kênh bán hàng theo yêu cầu
    @Column(name = "is_available_dinein")
    @Builder.Default
    private boolean isAvailableDineIn = true;

    @Column(name = "is_available_delivery")
    @Builder.Default
    private boolean isAvailableDelivery = true;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;

    // Phân loại thời gian phục vụ (MORNING, LUNCH, DINNER, NIGHT, ALL)
    // Mặc định là ALL (bán cả ngày)
    @Column(name = "meal_time")
    @Builder.Default
    private String mealTime = "ALL";
}