package ngo.cong.thao.s2o_pro.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ngo.cong.thao.s2o_pro.common.response.ApiResponse;
import ngo.cong.thao.s2o_pro.user.dto.StaffRequest;
import ngo.cong.thao.s2o_pro.user.dto.StaffResponse;
import ngo.cong.thao.s2o_pro.user.entity.User;
import ngo.cong.thao.s2o_pro.user.service.StaffService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenant/staff")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')") // Chỉ Chủ và Quản lý mới được vào khu vực này
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @PostMapping
    public ResponseEntity<ApiResponse<StaffResponse>> createStaff(@Valid @RequestBody StaffRequest request) {
        User staff = staffService.createStaff(request);
        return ResponseEntity.ok(ApiResponse.success(StaffResponse.fromEntity(staff)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StaffResponse>>> getStaffList() {
        List<StaffResponse> list = staffService.getStaffList().stream().map(StaffResponse::fromEntity).toList();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<ApiResponse<StaffResponse>> toggleStatus(@PathVariable UUID id) {
        User staff = staffService.toggleStaffStatus(id);
        return ResponseEntity.ok(ApiResponse.success(StaffResponse.fromEntity(staff)));
    }
}