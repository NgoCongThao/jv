package ngo.cong.thao.s2o_pro.menu.controller;

import jakarta.validation.Valid;
import ngo.cong.thao.s2o_pro.common.response.ApiResponse;
import ngo.cong.thao.s2o_pro.menu.dto.MenuItemRequest;
import ngo.cong.thao.s2o_pro.menu.dto.MenuItemResponse;
import ngo.cong.thao.s2o_pro.menu.entity.MenuCategory;
import ngo.cong.thao.s2o_pro.menu.entity.MenuItem;
import ngo.cong.thao.s2o_pro.menu.repository.MenuCategoryRepository;
import ngo.cong.thao.s2o_pro.menu.service.MenuService;
import ngo.cong.thao.s2o_pro.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private final MenuService menuService;
    private final MenuCategoryRepository menuCategoryRepository; // Tiện cho việc test tạo category

    public MenuController(MenuService menuService, MenuCategoryRepository menuCategoryRepository) {
        this.menuService = menuService;
        this.menuCategoryRepository = menuCategoryRepository;
    }

    // --- API TẠO DANH MỤC (Dùng tạm để có Category ID test tạo món) ---
    @PostMapping("/categories")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<MenuCategory>> createCategory(@RequestBody MenuCategory category) {
        category.setTenantId(TenantContext.getTenantId());
        MenuCategory savedCategory = menuCategoryRepository.save(category);
        return ResponseEntity.ok(ApiResponse.success(savedCategory));
    }

    // --- API TẠO MÓN ĂN ---
    @PostMapping("/items")
    @PreAuthorize("hasRole('RESTAURANT_OWNER') or hasRole('CHEF')")
    public ResponseEntity<ApiResponse<MenuItemResponse>> createMenuItem(@Valid @RequestBody MenuItemRequest request) {
        MenuItem menuItem = MenuItem.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .isAvailableDineIn(request.isAvailableDineIn())
                .isAvailableDelivery(request.isAvailableDelivery())
                .build();

        MenuItem savedItem = menuService.createMenuItem(menuItem, request.getCategoryId());
        return ResponseEntity.ok(ApiResponse.success(MenuItemResponse.fromEntity(savedItem)));
    }

    // --- API LẤY DANH SÁCH MÓN ĂN (Có phân trang & Redis) ---
    @GetMapping("/items")
    public ResponseEntity<ApiResponse<Page<MenuItemResponse>>> getMenuItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<MenuItem> itemPage = menuService.getAllMenuItems(PageRequest.of(page, size));
        Page<MenuItemResponse> responsePage = itemPage.map(MenuItemResponse::fromEntity);

        return ResponseEntity.ok(ApiResponse.success(responsePage));
    }
}