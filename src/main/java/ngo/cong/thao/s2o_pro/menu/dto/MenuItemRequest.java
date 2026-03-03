package ngo.cong.thao.s2o_pro.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class MenuItemRequest {
    @NotBlank(message = "Tên món không được để trống")
    private String name;

    private String description;

    @NotNull(message = "Giá không được để trống")
    private BigDecimal price;

    private String imageUrl;

    @NotNull(message = "ID danh mục không được để trống")
    private UUID categoryId;

    private boolean isAvailableDineIn = true;
    private boolean isAvailableDelivery = true;
}