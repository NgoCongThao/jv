package ngo.cong.thao.s2o_pro.user.repository;

import ngo.cong.thao.s2o_pro.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);

    java.util.List<ngo.cong.thao.s2o_pro.user.entity.User> findByTenantIdAndRoleIn(String tenantId, java.util.List<ngo.cong.thao.s2o_pro.user.entity.Role> roles);
}