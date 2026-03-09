package ngo.cong.thao.s2o_pro.delivery.repository;

import ngo.cong.thao.s2o_pro.delivery.entity.DeliveryTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryTicketRepository extends JpaRepository<DeliveryTicket, UUID> {
    Optional<DeliveryTicket> findByOrderId(UUID orderId);
}