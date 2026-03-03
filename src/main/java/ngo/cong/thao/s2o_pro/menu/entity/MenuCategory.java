package ngo.cong.thao.s2o_pro.menu.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;
import ngo.cong.thao.s2o_pro.common.entity.TenantAwareEntity;

@Entity
@Table(name = "menu_categories", indexes = {
        @Index(name = "idx_category_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuCategory extends TenantAwareEntity {

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;
}