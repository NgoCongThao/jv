package ngo.cong.thao.s2o_pro.menu.service;

import lombok.RequiredArgsConstructor;
import ngo.cong.thao.s2o_pro.menu.entity.MenuCategory;
import ngo.cong.thao.s2o_pro.menu.entity.MenuItem;
import ngo.cong.thao.s2o_pro.menu.repository.MenuCategoryRepository;
import ngo.cong.thao.s2o_pro.menu.repository.MenuItemRepository;
import ngo.cong.thao.s2o_pro.tenant.TenantContext;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor // Lombok tự động tạo constructor cho các field 'final'
public class MenuServiceImpl implements MenuService {

    private final MenuItemRepository menuItemRepository;
    private final MenuCategoryRepository menuCategoryRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "menu_items_v2", key = "T(ngo.cong.thao.s2o_pro.tenant.TenantContext).getTenantId() + '-' + #pageable.pageNumber", sync = true)
    public Page<MenuItem> getAllMenuItems(Pageable pageable) {
        // Lấy Page nguyên bản từ Database
        Page<MenuItem> page = menuItemRepository.findAll(pageable);

        // Bọc nó lại bằng CustomPageImpl để Redis có thể đọc hiểu
        return new ngo.cong.thao.s2o_pro.common.response.CustomPageImpl<>(
                page.getContent(),
                pageable,
                page.getTotalElements()
        );
    }
    // Khi tạo món mới, XÓA sạch cache của nhà hàng này (để user gọi lại API sẽ thấy dữ liệu mới nhất)
    @Override
    @Transactional
    @CacheEvict(value = "menu_items_v2", key = "T(ngo.cong.thao.s2o_pro.tenant.TenantContext).getTenantId() + '-*'", allEntries = true)
    public MenuItem createMenuItem(MenuItem menuItem, UUID categoryId) {
        MenuCategory category = menuCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục!"));

        menuItem.setCategory(category);

        // Gán cứng tenantId lấy từ Context để bảo mật kép
        menuItem.setTenantId(TenantContext.getTenantId());

        return menuItemRepository.save(menuItem);

    }
    // CẬP NHẬT TRẠNG  THÁI MÓN
    @Override
    @Transactional
    // Xóa cache để menu cập nhật ngay lập tức cho khách
    @CacheEvict(value = "menu_items_v2", key = "T(ngo.cong.thao.s2o_pro.tenant.TenantContext).getTenantId() + '-*'", allEntries = true)
    public MenuItem toggleMenuItemStatus(UUID id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy món ăn"));

        // Đảo ngược trạng thái: Nếu đang bán (true) thì thành Ngưng bán (false) và ngược lại
        item.setActive(!item.isActive());

        return menuItemRepository.save(item);
    }
}