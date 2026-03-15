package ngo.cong.thao.s2o_pro.ai.controller;

import ngo.cong.thao.s2o_pro.ai.service.RecommendationService;
import ngo.cong.thao.s2o_pro.common.response.ApiResponse;
import ngo.cong.thao.s2o_pro.menu.dto.MenuItemResponse;
import ngo.cong.thao.s2o_pro.menu.entity.MenuItem;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    // API Gợi ý món ăn theo giờ hiện tại
    @GetMapping("/time-based")
    @PreAuthorize("hasAnyRole('OWNER', 'CUSTOMER', 'GUEST')")
    public ResponseEntity<ApiResponse<List<MenuItemResponse>>> getRecommendations() {
        List<MenuItem> items = recommendationService.getRecommendationsByTime();

        // Chuyển Entity sang DTO để trả về cho Client
        List<MenuItemResponse> response = items.stream()
                .map(MenuItemResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}