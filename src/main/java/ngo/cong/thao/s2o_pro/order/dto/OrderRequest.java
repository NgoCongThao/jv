package ngo.cong.thao.s2o_pro.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ngo.cong.thao.s2o_pro.order.entity.OrderType;

import java.util.List;

@Data
public class OrderRequest {

    @NotNull(message = "Loại đơn hàng không được để trống")
    private OrderType orderType;

    // Các trường này có thể null, ta sẽ validate bằng code logic tùy theo OrderType
    private String tableId;
    private String customerName;
    private String customerPhone;
    private String deliveryAddress;
    private String deliveryNotes;

    @NotEmpty(message = "Đơn hàng phải có ít nhất 1 món")
    @Valid
    private List<OrderItemRequest> items;
}