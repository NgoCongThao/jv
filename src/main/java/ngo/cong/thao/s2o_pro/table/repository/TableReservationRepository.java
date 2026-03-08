package ngo.cong.thao.s2o_pro.table.repository;

import ngo.cong.thao.s2o_pro.table.entity.TableReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface TableReservationRepository extends JpaRepository<TableReservation, UUID> {

    // Kiểm tra xem bàn này có ai đặt trong khoảng thời gian [start, end] chưa (Chỉ tính các đơn CONFIRMED)
    @Query("SELECT COUNT(r) > 0 FROM TableReservation r WHERE r.tableId = :tableId AND r.status = 'CONFIRMED' AND r.reservationTime >= :start AND r.reservationTime <= :end")
    boolean isTableReservedInTimeSlot(@Param("tableId") UUID tableId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}