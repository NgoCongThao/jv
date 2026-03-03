package ngo.cong.thao.s2o_pro.ai.service;

import lombok.RequiredArgsConstructor;
import ngo.cong.thao.s2o_pro.menu.entity.MenuItem;
import ngo.cong.thao.s2o_pro.menu.repository.MenuItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final MenuItemRepository menuItemRepository;

    @Transactional(readOnly = true)
    public List<MenuItem> getRecommendationsByTime() {
        // 1. Lấy giờ hiện tại của hệ thống
        int currentHour = LocalTime.now().getHour();
        String currentPeriod;

        // 2. Phân loại mốc thời gian
        if (currentHour >= 5 && currentHour < 10) {
            currentPeriod = "MORNING";
        } else if (currentHour >= 10 && currentHour < 14) {
            currentPeriod = "LUNCH";
        } else if (currentHour >= 14 && currentHour < 22) {
            currentPeriod = "DINNER";
        } else {
            currentPeriod = "NIGHT";
        }

        // 3. Lấy TẤT CẢ món ăn của nhà hàng hiện tại (TenantFilter tự động chạy)
        List<MenuItem> allItems = menuItemRepository.findAll();

        // 4. Lọc ra các món hợp với buổi hiện tại HOẶC bán cả ngày ("ALL")
        List<MenuItem> recommendedItems = allItems.stream()
                .filter(item -> item.isActive() &&
                        (item.getMealTime().contains(currentPeriod) || item.getMealTime().contains("ALL")))
                .collect(Collectors.toList());

        // 5. Xáo trộn danh sách (để mỗi lần mở app gợi ý một kiểu) và lấy tối đa 5 món
        Collections.shuffle(recommendedItems);
        return recommendedItems.stream().limit(5).collect(Collectors.toList());
    }
}