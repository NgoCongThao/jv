package ngo.cong.thao.s2o_pro.user.repository;

import ngo.cong.thao.s2o_pro.user.entity.CustomerMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerMembershipRepository extends JpaRepository<CustomerMembership, UUID> {
    Optional<CustomerMembership> findByCustomerIdAndTenantId(UUID customerId, String tenantId);
}