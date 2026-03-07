package ngo.cong.thao.s2o_pro.table.repository;

import ngo.cong.thao.s2o_pro.table.entity.DiningTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DiningTableRepository extends JpaRepository<DiningTable, UUID> {
    // Các query mặc định đã bị chặn bởi TenantFilterAspect của bạn, rất an toàn!
}