package ngo.cong.thao.s2o_pro.table.service;

import lombok.RequiredArgsConstructor;
import ngo.cong.thao.s2o_pro.common.service.QrCodeService;
import ngo.cong.thao.s2o_pro.table.entity.DiningTable;
import ngo.cong.thao.s2o_pro.table.repository.DiningTableRepository;
import ngo.cong.thao.s2o_pro.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiningTableServiceImpl implements DiningTableService {

    private final DiningTableRepository tableRepository;
    private final QrCodeService qrCodeService;
    private final ngo.cong.thao.s2o_pro.table.repository.TableReservationRepository reservationRepository;
    @Override
    @Transactional
    public DiningTable createTable(String tableName) {
        // 1. Lấy mã nhà hàng (Tenant ID)
        String tenantId = TenantContext.getTenantId();

        // 2. Tạo record Bàn mới (chưa có QR)
        DiningTable table = DiningTable.builder()
                .tableName(tableName)
                .build();
        table.setTenantId(tenantId);
        DiningTable savedTable = tableRepository.save(table);

        // 3. Chuẩn bị nội dung mã QR (URL để khách hàng quét và mở App Web)
        // Link này trỏ thẳng về Front-end của bạn
        String qrContent = String.format("https://s2o-pro.vn/order?tenantId=%s&tableId=%s",
                tenantId, savedTable.getId());

        // 4. Sinh mã QR dạng Base64 và cập nhật lại vào DB
        try {
            String base64Qr = qrCodeService.generateQrCodeBase64(qrContent, 300, 300);
            savedTable.setQrCodeBase64("data:image/png;base64," + base64Qr);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi trong quá trình sinh mã QR cho bàn", e);
        }

        return tableRepository.save(savedTable);
    }
    @Override
    @Transactional
    public ngo.cong.thao.s2o_pro.table.entity.TableReservation reserveTable(java.util.UUID tableId, String customerName, String phone, java.time.LocalDateTime time, int guestCount) {

        // 1. Kiểm tra xem bàn có tồn tại không
        DiningTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Bàn này!"));

        // 2. Thuật toán chống trùng lịch: Khoanh vùng an toàn 2 tiếng trước và sau giờ đặt
        java.time.LocalDateTime safeStartTime = time.minusHours(2);
        java.time.LocalDateTime safeEndTime = time.plusHours(2);

        boolean isConflict = reservationRepository.isTableReservedInTimeSlot(tableId, safeStartTime, safeEndTime);

        if (isConflict) {
            throw new IllegalStateException("Bàn này đã có khách đặt trước trong khoảng thời gian từ " +
                    safeStartTime.toLocalTime() + " đến " + safeEndTime.toLocalTime() + ". Vui lòng chọn bàn khác hoặc giờ khác!");
        }

        // 3. Nếu an toàn -> Tạo phiếu Đặt bàn
        ngo.cong.thao.s2o_pro.table.entity.TableReservation reservation = ngo.cong.thao.s2o_pro.table.entity.TableReservation.builder()
                .tableId(tableId)
                .customerName(customerName)
                .customerPhone(phone)
                .reservationTime(time)
                .guestCount(guestCount)
                .build();

        reservation.setTenantId(ngo.cong.thao.s2o_pro.tenant.TenantContext.getTenantId());

        // 4. Đổi trạng thái Bàn cực kỳ thông minh
        // Chỉ khóa bàn (RESERVED) nếu khách đến trong vòng 2 tiếng nữa.
        // Nếu khách đặt xa hơn (VD: Đặt 19h, hiện tại 14h), vẫn để bàn AVAILABLE cho khách khác ngồi.
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (time.minusHours(2).isBefore(now)) {
            table.setStatus(DiningTable.TableStatus.RESERVED);
            tableRepository.save(table);
        }

        return reservationRepository.save(reservation);
    }
}