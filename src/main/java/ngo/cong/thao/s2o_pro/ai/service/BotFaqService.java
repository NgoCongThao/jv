package ngo.cong.thao.s2o_pro.ai.service;

import ngo.cong.thao.s2o_pro.ai.entity.BotFaq;
import java.util.List;

public interface BotFaqService {
    BotFaq createFaq(BotFaq botFaq);
    List<BotFaq> getAllFaqs();
    String getAnswer(String userQuestion); // Hàm cốt lõi của con Bot
}