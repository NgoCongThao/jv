package ngo.cong.thao.s2o_pro.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BotChatRequest {
    @NotBlank(message = "Câu hỏi không được để trống")
    private String question;
}