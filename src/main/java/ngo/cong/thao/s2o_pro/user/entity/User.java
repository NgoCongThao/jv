package ngo.cong.thao.s2o_pro.user.entity;

import jakarta.persistence.*;
import lombok.*;
import ngo.cong.thao.s2o_pro.common.entity.BaseEntity;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // Cột này cho phép null. Nếu là PLATFORM_ADMIN thì giá trị sẽ là null.
    // Nếu là nhân viên/chủ của nhà hàng nào, giá trị sẽ là ID của nhà hàng đó.
    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;
}