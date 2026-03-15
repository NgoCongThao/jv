package ngo.cong.thao.s2o_pro.tenant.controller;

import lombok.RequiredArgsConstructor;
import ngo.cong.thao.s2o_pro.common.response.ApiResponse;
import ngo.cong.thao.s2o_pro.tenant.dto.TenantRegisterRequest;
import ngo.cong.thao.s2o_pro.tenant.service.TenantService;
import ngo.cong.thao.s2o_pro.user.entity.Tenant;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    // 1. API PUBLIC: Cho phép khách vãng lai (chủ quán tương lai) điền form đăng ký
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Tenant>> register(@RequestBody TenantRegisterRequest request) {
        Tenant tenant = tenantService.registerTenant(request);
        return ResponseEntity.ok(ApiResponse.success(tenant));
    }

    // 2. API ADMIN: Chỉ có ADMIN hệ thống S2O mới được quyền gọi nút Duyệt
    @PostMapping("/{tenantId}/approve")
    @PreAuthorize("hasRole('ADMIN')") // Chặn ngay ở cửa, ai không phải ADMIN là báo lỗi 403
    public ResponseEntity<ApiResponse<String>> approveTenant(@PathVariable String tenantId) {
        String result = tenantService.approveTenant(tenantId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}