package ngo.cong.thao.s2o_pro.tenant.repository;

import ngo.cong.thao.s2o_pro.user.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findByDomain(String domain);}