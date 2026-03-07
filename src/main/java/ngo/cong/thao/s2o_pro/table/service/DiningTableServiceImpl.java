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
}