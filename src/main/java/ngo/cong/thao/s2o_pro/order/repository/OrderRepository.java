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
    // Tính tổng số đơn hàng theo trạng thái và khoảng thời gian
    long countByStatusAndCreatedAtBetween(ngo.cong.thao.s2o_pro.order.entity.OrderStatus status, java.time.LocalDateTime start, java.time.LocalDateTime end);

    // Tính tổng doanh thu (SUM totalAmount)
    @org.springframework.data.jpa.repository.Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = :status AND o.createdAt >= :start AND o.createdAt <= :end")
    java.math.BigDecimal sumRevenue(@org.springframework.data.repository.query.Param("status") ngo.cong.thao.s2o_pro.order.entity.OrderStatus status,
                                    @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
                                    @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);
    // 2. Tính tổng doanh thu theo NGUỒN (DINE_IN / DELIVERY)
    @org.springframework.data.jpa.repository.Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = :status AND o.orderType = :type AND o.createdAt >= :start AND o.createdAt <= :end")
    java.math.BigDecimal sumRevenueByType(
            @org.springframework.data.repository.query.Param("status") ngo.cong.thao.s2o_pro.order.entity.OrderStatus status,
            @org.springframework.data.repository.query.Param("type") ngo.cong.thao.s2o_pro.order.entity.OrderType type,
            @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
            @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end
    );

    // 3. Báo cáo TỐC ĐỘ TIÊU THỤ MÓN ĂN (Sắp xếp từ bán chạy nhất xuống thấp nhất)
    @org.springframework.data.jpa.repository.Query("SELECT new ngo.cong.thao.s2o_pro.order.dto.ItemSalesDto(i.menuItem.name, SUM(CAST(i.quantity AS long)), SUM(i.unitPrice * i.quantity)) " +
            "FROM OrderItem i JOIN i.order o " +
            "WHERE o.status = :status AND o.createdAt >= :start AND o.createdAt <= :end " +
            "GROUP BY i.menuItem.name ORDER BY SUM(i.quantity) DESC")
    java.util.List<ngo.cong.thao.s2o_pro.order.dto.ItemSalesDto> getTopSellingItems(
            @org.springframework.data.repository.query.Param("status") ngo.cong.thao.s2o_pro.order.entity.OrderStatus status,
            @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
            @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end,
            org.springframework.data.domain.Pageable pageable // Dùng để limit lấy top 5 hoặc top 10

    );
    // Tìm đơn hàng đang hoạt động (Chưa PAID, chưa CANCELLED) của một bàn cụ thể
    @org.springframework.data.jpa.repository.Query("SELECT o FROM Order o WHERE o.tableId = :tableId AND o.status NOT IN ('PAID', 'CANCELLED')")
    java.util.Optional<ngo.cong.thao.s2o_pro.order.entity.Order> findActiveOrderByTableId(@org.springframework.data.repository.query.Param("tableId") String tableId);
}