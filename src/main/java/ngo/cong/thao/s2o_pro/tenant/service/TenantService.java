package ngo.cong.thao.s2o_pro.tenant.service;

import ngo.cong.thao.s2o_pro.tenant.dto.TenantRegisterRequest;
import ngo.cong.thao.s2o_pro.user.entity.Tenant;



public interface TenantService {
    // Chủ quán tự đăng ký
    Tenant registerTenant(TenantRegisterRequest request);

    // Admin duyệt quán
    String approveTenant(String tenantId);

}