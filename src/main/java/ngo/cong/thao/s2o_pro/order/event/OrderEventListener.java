package ngo.cong.thao.s2o_pro.order.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ngo.cong.thao.s2o_pro.table.entity.DiningTable;
import ngo.cong.thao.s2o_pro.table.repository.DiningTableRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    // Inject cái chổi để đi dọn bàn
    private final DiningTableRepository diningTableRepository;

    @EventListener
    @Transactional // Rất quan trọng: Đảm bảo thao tác lưu Bàn nằm trong 1 Transaction an toàn
    public void handleOrderPaidEvent(OrderPaidEvent event) {
        log.info("🔥 [EVENT CATCHED] Đơn hàng {} của nhà hàng {} vừa thanh toán thành công. Tổng tiền: {}.",
                event.orderId(), event.tenantId(), event.amount());

        // Nếu đơn hàng này ăn tại bàn (có tableId)
        if (event.tableId() != null && !event.tableId().isEmpty()) {

            // Tìm bàn (Thử tìm theo UUID trước, nếu lỗi thì tìm theo Tên bàn)
            DiningTable table = null;
            try {
                UUID tableUuid = UUID.fromString(event.tableId());
                table = diningTableRepository.findById(tableUuid).orElse(null);
            } catch (IllegalArgumentException e) {
                // Khách đang test truyền chữ "Ban_VIP_01", ta tìm theo Tên bàn
                table = diningTableRepository.findByTableName(event.tableId()).orElse(null);
            }

            // Đổi trạng thái bàn thành Trống (AVAILABLE)
            if (table != null) {
                table.setStatus(DiningTable.TableStatus.AVAILABLE);
                diningTableRepository.save(table);
                log.info("🧹 [CLEANUP] Đã tự động giải phóng bàn [{}] về trạng thái AVAILABLE.", table.getTableName());
            } else {
                log.warn("⚠️ Không tìm thấy bàn [{}] để giải phóng!", event.tableId());
            }
        }
    }
}