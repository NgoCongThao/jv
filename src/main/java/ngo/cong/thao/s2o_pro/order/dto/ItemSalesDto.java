package ngo.cong.thao.s2o_pro.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemSalesDto {
    private String itemName;
    private Long quantitySold; // Tổng số lượng đã bán
    private BigDecimal totalRevenue; // Doanh thu mang lại từ món này
}