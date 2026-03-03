package ngo.cong.thao.s2o_pro.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class OrderItemRequest {
    @NotNull(message = "ID món ăn không được để trống")
    private UUID menuItemId;

    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private int quantity;

    private String notes;
}