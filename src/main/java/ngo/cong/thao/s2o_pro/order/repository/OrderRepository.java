package ngo.cong.thao.s2o_pro.order.repository;

import ngo.cong.thao.s2o_pro.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
}