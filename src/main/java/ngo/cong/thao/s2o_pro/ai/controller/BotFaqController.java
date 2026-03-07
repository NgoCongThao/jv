package ngo.cong.thao.s2o_pro.ai.controller;

import jakarta.validation.Valid;
import ngo.cong.thao.s2o_pro.ai.dto.BotChatRequest;
import ngo.cong.thao.s2o_pro.ai.entity.BotFaq;
import ngo.cong.thao.s2o_pro.ai.service.BotFaqService;
import ngo.cong.thao.s2o_pro.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bot")
public class BotFaqController {

    private final BotFaqService botFaqService;

    public BotFaqController(BotFaqService botFaqService) {
        this.botFaqService = botFaqService;
    }

    // --- API 1: Dành cho Chủ quán "dạy" Bot ---
    @PostMapping("/faqs")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<BotFaq>> createFaq(@RequestBody BotFaq botFaq) {
        BotFaq savedFaq = botFaqService.createFaq(botFaq);
        return ResponseEntity.ok(ApiResponse.success(savedFaq));
    }

    // --- API 2: Dành cho Khách hàng Chat với Bot ---
    @PostMapping("/chat")
    @PreAuthorize("hasAnyRole('OWNER', 'CUSTOMER', 'GUEST')")
    public ResponseEntity<ApiResponse<String>> chatWithBot(@Valid @RequestBody BotChatRequest request) {
        String answer = botFaqService.getAnswer(request.getQuestion());
        return ResponseEntity.ok(ApiResponse.success(answer));
    }
}