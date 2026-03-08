package ngo.cong.thao.s2o_pro.order.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardSummaryResponse {
    // 1. Tổng quan
    private BigDecimal totalRevenue;
    private long totalOrders;

    // 2. Phân tích nguồn thu
    private BigDecimal dineInRevenue;   // Doanh thu ăn tại bàn
    private BigDecimal deliveryRevenue; // Doanh thu giao hàng

    // 3. Phân tích món ăn
    private List<ItemSalesDto> topSellingItems; // Món bán chạy nhất
}