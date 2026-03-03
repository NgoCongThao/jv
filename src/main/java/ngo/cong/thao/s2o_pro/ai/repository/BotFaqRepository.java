package ngo.cong.thao.s2o_pro.ai.repository;

import ngo.cong.thao.s2o_pro.ai.entity.BotFaq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BotFaqRepository extends JpaRepository<BotFaq, UUID> {
    // Tìm tất cả các câu hỏi đang active của nhà hàng hiện tại (TenantFilter sẽ tự động lo phần tenantId)
    List<BotFaq> findAllByIsActiveTrue();
}