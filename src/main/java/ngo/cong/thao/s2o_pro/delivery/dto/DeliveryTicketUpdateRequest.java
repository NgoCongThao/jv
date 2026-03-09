package ngo.cong.thao.s2o_pro.delivery.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DeliveryTicketUpdateRequest {
    private String shipperName;    // Tên tài xế (VD: Nguyễn Văn A)
    private String shipperPhone;   // SĐT tài xế
    private String licensePlate;   // Biển số xe (VD: 59-X1 12345)
    private BigDecimal deliveryFee; // Phí ship
}