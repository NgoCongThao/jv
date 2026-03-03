package ngo.cong.thao.s2o_pro.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import ngo.cong.thao.s2o_pro.common.entity.TenantAwareEntity;

@Entity
@Table(name = "bot_faqs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BotFaq extends TenantAwareEntity {

    @Column(nullable = false)
    private String question; // Câu hỏi mẫu (VD: Quán có pass wifi là gì?)

    @Column(nullable = false, length = 1000)
    private String answer; // Câu trả lời của bot

    @Column(nullable = false)
    private String keywords; // Các từ khóa để bot nhận diện, cách nhau bằng dấu phẩy (VD: "wifi,mạng,internet,mật khẩu")

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;
}