package ngo.cong.thao.s2o_pro.ai.service;

import lombok.RequiredArgsConstructor;
import ngo.cong.thao.s2o_pro.ai.entity.BotFaq;
import ngo.cong.thao.s2o_pro.ai.repository.BotFaqRepository;
import ngo.cong.thao.s2o_pro.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class BotFaqServiceImpl implements BotFaqService {

    private final BotFaqRepository botFaqRepository;

    @Override
    public BotFaq createFaq(BotFaq botFaq) {
        botFaq.setTenantId(TenantContext.getTenantId());
        return botFaqRepository.save(botFaq);
    }

    @Override
    public List<BotFaq> getAllFaqs() {
        return botFaqRepository.findAllByIsActiveTrue();
    }

    @Override
    public String getAnswer(String userQuestion) {
        if (userQuestion == null || userQuestion.trim().isEmpty()) {
            return "Dạ, bạn cần hỗ trợ thông tin gì ạ?";
        }

        // 1. Chuẩn hóa câu hỏi của khách: bỏ dấu, viết thường
        String normalizedQuestion = removeVietnameseAccents(userQuestion);

        List<BotFaq> faqs = botFaqRepository.findAllByIsActiveTrue();

        int maxScore = 0;
        BotFaq bestMatch = null;

        // 2. Thuật toán chấm điểm (Scoring Algorithm)
        for (BotFaq faq : faqs) {
            int score = 0;
            // Tách các từ khóa lưu trong DB (ngăn cách bởi dấu phẩy)
            String[] keywords = faq.getKeywords().split(",");

            for (String kw : keywords) {
                String normalizedKw = removeVietnameseAccents(kw.trim());
                if (normalizedKw.isEmpty()) continue;

                // Nếu câu hỏi của khách chứa từ khóa này -> Cộng 1 điểm
                if (normalizedQuestion.contains(normalizedKw)) {
                    score++;
                }
            }

            // Cập nhật câu trả lời có điểm cao nhất
            if (score > maxScore) {
                maxScore = score;
                bestMatch = faq;
            }
        }

        // 3. Trả về kết quả
        if (maxScore > 0 && bestMatch != null) {
            return bestMatch.getAnswer();
        } else {
            return "Dạ xin lỗi, hiện tại em chưa hiểu ý của bạn. Bạn có thể hỏi cụ thể hơn về Menu, Giờ mở cửa, Mật khẩu Wifi, hoặc Địa chỉ quán được không ạ?";
        }
    }

    // --- Hàm tiện ích: Chuyển "Tiếng Việt" thành "tieng viet" ---
    private String removeVietnameseAccents(String str) {
        try {
            String temp = Normalizer.normalize(str, Normalizer.Form.NFD);
            Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
            String noAccent = pattern.matcher(temp).replaceAll("");
            return noAccent.replace('đ', 'd').replace('Đ', 'D').toLowerCase();
        } catch (Exception e) {
            return str.toLowerCase();
        }
    }
}