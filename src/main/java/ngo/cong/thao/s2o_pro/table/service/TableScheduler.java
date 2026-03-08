package ngo.cong.thao.s2o_pro.table.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ngo.cong.thao.s2o_pro.table.entity.DiningTable;
import ngo.cong.thao.s2o_pro.table.entity.TableReservation;
import ngo.cong.thao.s2o_pro.table.repository.DiningTableRepository;
import ngo.cong.thao.s2o_pro.table.repository.TableReservationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TableScheduler {

    private final TableReservationRepository reservationRepository;
    private final DiningTableRepository tableRepository;

    // 1. ROBOT KHÓA BÀN: Chạy mỗi 1 phút một lần để kiểm tra các bàn sắp đến giờ nhận
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void lockUpcomingReservations() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twoHoursLater = now.plusHours(2);

        // Tìm tất cả các đơn Đặt bàn đang CONFIRMED và sẽ đến trong 2 tiếng tới
        List<TableReservation> upcomingReservations = reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() == TableReservation.ReservationStatus.CONFIRMED)
                .filter(r -> r.getReservationTime().isAfter(now) && r.getReservationTime().isBefore(twoHoursLater))
                .toList();

        for (TableReservation res : upcomingReservations) {
            tableRepository.findById(res.getTableId()).ifPresent(table -> {
                // Nếu bàn đang trống, thì Khóa lại thành RESERVED để chờ khách
                if (table.getStatus() == DiningTable.TableStatus.AVAILABLE) {
                    table.setStatus(DiningTable.TableStatus.RESERVED);
                    tableRepository.save(table);
                    log.info("⏰ [CRON JOB] Đã tự động khóa bàn [{}] chờ khách đặt lúc {}", table.getTableName(), res.getReservationTime());
                }
            });
        }
    }

    // 2. ROBOT HỦY BÀN (No-show): Chạy mỗi 1 phút, Hủy các bàn khách trễ hẹn quá 30 phút
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void cancelOverdueReservations() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyMinsAgo = now.minusMinutes(30);

        List<TableReservation> overdueReservations = reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() == TableReservation.ReservationStatus.CONFIRMED)
                .filter(r -> r.getReservationTime().isBefore(thirtyMinsAgo)) // Đã quá giờ 30 phút
                .toList();

        for (TableReservation res : overdueReservations) {
            // Đánh dấu Hủy phiếu đặt
            res.setStatus(TableReservation.ReservationStatus.CANCELLED);
            reservationRepository.save(res);

            // Mở lại bàn thành Trống
            tableRepository.findById(res.getTableId()).ifPresent(table -> {
                if (table.getStatus() == DiningTable.TableStatus.RESERVED) {
                    table.setStatus(DiningTable.TableStatus.AVAILABLE);
                    tableRepository.save(table);
                    log.warn("❌ [CRON JOB] Khách trễ 30 phút. Đã tự động HỦY đơn đặt và giải phóng bàn [{}].", table.getTableName());
                }
            });
        }
    }
}