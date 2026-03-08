package ngo.cong.thao.s2o_pro.menu.service;

import ngo.cong.thao.s2o_pro.menu.entity.MenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MenuService {
    Page<MenuItem> getAllMenuItems(Pageable pageable);
    MenuItem createMenuItem(MenuItem menuItem, UUID categoryId);
    MenuItem toggleMenuItemStatus(java.util.UUID id);
}