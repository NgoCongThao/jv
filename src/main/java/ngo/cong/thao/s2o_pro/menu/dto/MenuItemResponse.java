package ngo.cong.thao.s2o_pro.menu.dto;

import lombok.Builder;
import lombok.Data;
import ngo.cong.thao.s2o_pro.menu.entity.MenuItem;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class MenuItemResponse {
    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private String categoryName;
    private boolean isAvailableDineIn;
    private boolean isAvailableDelivery;

    // Hàm tiện ích chuyển từ Entity sang DTO
    public static MenuItemResponse fromEntity(MenuItem entity) {
        return MenuItemResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .categoryName(entity.getCategory() != null ? entity.getCategory().getName() : null)
                .isAvailableDineIn(entity.isAvailableDineIn())
                .isAvailableDelivery(entity.isAvailableDelivery())
                .build();
    }
}