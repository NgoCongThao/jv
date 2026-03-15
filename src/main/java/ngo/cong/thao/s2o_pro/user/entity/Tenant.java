package ngo.cong.thao.s2o_pro.user.entity;

import jakarta.persistence.*;
import lombok.*;
import ngo.cong.thao.s2o_pro.common.entity.BaseEntity;
import ngo.cong.thao.s2o_pro.tenant.entity.TenantStatus;

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
    @Column(name = "owner_email")
    private String ownerEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TenantStatus status;

}