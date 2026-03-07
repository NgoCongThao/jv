package ngo.cong.thao.s2o_pro.order.repository;

import ngo.cong.thao.s2o_pro.order.entity.Order;
import ngo.cong.thao.s2o_pro.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    // THÊM DÒNG NÀY: Tìm các đơn hàng nằm trong danh sách trạng thái, sắp xếp theo thời gian tạo cũ nhất lên đầu (để bếp làm trước)
    Page<Order> findAllByStatusInOrderByCreatedAtAsc(List<OrderStatus> statuses, Pageable pageable);
}