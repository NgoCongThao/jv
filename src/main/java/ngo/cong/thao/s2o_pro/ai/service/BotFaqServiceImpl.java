package ngo.cong.thao.s2o_pro.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ngo.cong.thao.s2o_pro.ai.dto.GeminiRequest;
import ngo.cong.thao.s2o_pro.menu.entity.MenuItem;
import ngo.cong.thao.s2o_pro.menu.repository.MenuItemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotFaqServiceImpl implements BotFaqService {

    // Tiêm các Repository đã có sẵn để lấy thông tin
    private final ngo.cong.thao.s2o_pro.ai.repository.BotFaqRepository botFaqRepository;
    private final MenuItemRepository menuItemRepository;
    // ... các repository cũ
    private final ngo.cong.thao.s2o_pro.tenant.repository.TenantRepository tenantRepository;
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public ngo.cong.thao.s2o_pro.ai.entity.BotFaq createFaq(ngo.cong.thao.s2o_pro.ai.entity.BotFaq botFaq) {
        botFaq.setTenantId(ngo.cong.thao.s2o_pro.tenant.TenantContext.getTenantId());
        return botFaqRepository.save(botFaq);
    }

    @Override
    public List<ngo.cong.thao.s2o_pro.ai.entity.BotFaq> getAllFaqs() {
        return botFaqRepository.findAllByIsActiveTrue();
    }

    @Override
    public String getAnswer(String userQuestion) {
        if (userQuestion == null || userQuestion.trim().isEmpty()) {
            return "Dạ, em có thể giúp gì cho anh/chị ạ?";
        }

        try {
            // LẤY ĐỊNH DANH ĐỂ BIẾT KHÁCH ĐANG Ở TRANG CHỦ HAY TRANG NHÀ HÀNG
            String currentTenantId = ngo.cong.thao.s2o_pro.tenant.TenantContext.getTenantId();
            String prompt;

            // ==========================================
            // KỊCH BẢN 1: KHÁCH ĐANG Ở TRANG CHỦ HỆ THỐNG
            // ==========================================
            // ==========================================
            // KỊCH BẢN 1: KHÁCH ĐANG Ở TRANG CHỦ HỆ THỐNG
            // ==========================================
            if (currentTenantId == null) {
                // Đã sửa: Trỏ đúng về user.entity.Tenant và lọc theo status ACTIVE thay vì isActive()
                List<ngo.cong.thao.s2o_pro.user.entity.Tenant> tenants = tenantRepository.findAll().stream()
                        .filter(t -> t.getStatus() != null && "ACTIVE".equals(t.getStatus().name()))
                        .toList();

                String restaurantList = tenants.stream()
                        .map(t -> "- " + t.getName() + " (Địa chỉ: " + t.getAddress() + ")")
                        .collect(Collectors.joining("\n"));

                prompt = String.format(
                        "Bạn là AI Trợ lý Tổng đài của nền tảng quản lý nhà hàng S2O Pro. Nhiệm vụ của bạn là tư vấn cho khách truy cập.\n" +
                                "Dưới đây là danh sách các nhà hàng đang sử dụng hệ thống của chúng tôi:\n%s\n\n" +
                                "Khách hàng vừa hỏi: \"%s\"\n" +
                                "Hãy trả lời lịch sự, ngắn gọn. Nếu khách hỏi ăn gì, hãy gợi ý họ chọn một trong các nhà hàng trên. Nếu khách muốn đăng ký mở quán, hãy hướng dẫn họ liên hệ ban quản trị.",
                        restaurantList, userQuestion
                );
            }
            // ==========================================
            // KỊCH BẢN 2: KHÁCH ĐANG Ở TRONG MỘT NHÀ HÀNG
            // ==========================================
            else {
                // Nhờ TenantFilterAspect, lệnh findAll() tự động chỉ lấy món của đúng nhà hàng hiện tại
                List<MenuItem> menu = menuItemRepository.findAll();
                String menuInfo = menu.stream()
                        .filter(MenuItem::isActive)
                        .map(m -> "- " + m.getName() + ": " + m.getPrice() + " VNĐ")
                        .collect(Collectors.joining("\n"));

                String faqInfo = botFaqRepository.findAllByIsActiveTrue().stream()
                        .map(faq -> "Hỏi: " + faq.getQuestion() + " -> Đáp: " + faq.getAnswer())
                        .collect(Collectors.joining("\n"));

                prompt = String.format(
                        "Bạn là nhân viên AI phục vụ của nhà hàng (hiện tại). Nhiệm vụ của bạn là tư vấn thực đơn và giải đáp thắc mắc.\n" +
                                "Thực đơn của quán:\n%s\n\n" +
                                "Thông tin FAQ của quán:\n%s\n\n" +
                                "Khách hàng hỏi: \"%s\"\n" +
                                "Hãy trả lời khách dựa trên thông tin trên. Không bịa đặt thêm món ăn.",
                        menuInfo, faqInfo, userQuestion
                );
            }

            // Gọi API Gemini với cái Prompt đã được nhào nặn theo đúng ngữ cảnh
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;
            GeminiRequest requestBody = new GeminiRequest(prompt);
            Map<String, Object> response = restTemplate.postForObject(url, requestBody, Map.class);

            // Bóc tách kết quả... (Phần code bóc tách JSON giữ nguyên như cũ)
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                return (String) parts.get(0).get("text");
            }

            return "Dạ, hệ thống tư vấn đang bận một chút!";

        } catch (Exception e) {
            log.error("Lỗi khi gọi API Gemini: ", e);
            return "Dạ xin lỗi, em đang gặp chút sự cố kết nối!";
        }
    }
}