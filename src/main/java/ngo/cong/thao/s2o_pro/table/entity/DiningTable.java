package ngo.cong.thao.s2o_pro.table.entity;

import jakarta.persistence.*;
import lombok.*;
import ngo.cong.thao.s2o_pro.common.entity.TenantAwareEntity;

@Entity
@Table(name = "dining_tables", indexes = {
        @Index(name = "idx_table_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiningTable extends TenantAwareEntity {

    @Column(nullable = false)
    private String tableName; // VD: Bàn số 1, VIP 01, Sân vườn 2

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TableStatus status = TableStatus.AVAILABLE;

    @Column(columnDefinition = "TEXT") // Dùng TEXT vì chuỗi Base64 của ảnh QR rất dài
    private String qrCodeBase64;

    public enum TableStatus {
        AVAILABLE,  // Bàn trống
        OCCUPIED,   // Đang có khách
        RESERVED,   // Đã đặt trước
        HIDDEN      // Ẩn (Bàn đang sửa chữa)
    }
}