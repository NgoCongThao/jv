package ngo.cong.thao.s2o_pro.tenant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import ngo.cong.thao.s2o_pro.common.entity.BaseEntity;

@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String domain; // Ví dụ: s2o_nhahangA để nhận diện khi login

    private String address;

    private String phone;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;
}