package ngo.cong.thao.s2o_pro.user.service;

import lombok.RequiredArgsConstructor;
import ngo.cong.thao.s2o_pro.tenant.TenantContext;
import ngo.cong.thao.s2o_pro.user.dto.StaffRequest;
import ngo.cong.thao.s2o_pro.user.entity.Role;
import ngo.cong.thao.s2o_pro.user.entity.User;
import ngo.cong.thao.s2o_pro.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User createStaff(StaffRequest request) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new IllegalArgumentException("Lỗi: Không xác định được nhà hàng!");

        // 1. KIỂM TRA PHÂN CẤP QUYỀN HẠN
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isManager = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));

        if (request.getRole() == Role.ADMIN || request.getRole() == Role.OWNER || request.getRole() == Role.CUSTOMER) {
            throw new IllegalArgumentException("Lạm quyền: Bạn không được phép tạo tài khoản với Chức vụ này!");
        }
        if (isManager && request.getRole() == Role.MANAGER) {
            throw new IllegalArgumentException("Lạm quyền: Quản lý không được phép tạo thêm Quản lý khác!");
        }

        // 2. CHỐNG TRÙNG LẶP USERNAME (Tự động nối Domain)
        String fullUsername = tenantId + "_" + request.getUsernamePrefix().trim().toLowerCase();

        if (userRepository.findByUsername(fullUsername).isPresent()) {
            throw new IllegalArgumentException("Tên đăng nhập '" + fullUsername + "' đã tồn tại!");
        }

        // 3. TẠO TÀI KHOẢN
        User staff = User.builder()
                .fullName(request.getFullName())
                .username(fullUsername)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .tenantId(tenantId)
                .isActive(true)
                .build();

        return userRepository.save(staff);
    }

    @Override
    public List<User> getStaffList() {
        String tenantId = TenantContext.getTenantId();
        // Chỉ lấy nhân viên (Quản lý, Bếp, Thu ngân), không lấy Chủ quán hay Khách
        return userRepository.findByTenantIdAndRoleIn(tenantId, List.of(Role.MANAGER, Role.CHEF, Role.CASHIER));
    }

    @Override
    @Transactional
    public User toggleStaffStatus(UUID id) {
        User staff = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên!"));

        // CHỐT CHẶN BẢO MẬT KÉP
        if (!staff.getTenantId().equals(TenantContext.getTenantId())) {
            throw new IllegalArgumentException("Hành vi đáng ngờ: Không được sửa nhân viên của nhà hàng khác!");
        }
        if (staff.getRole() == Role.OWNER) {
            throw new IllegalArgumentException("Lỗi: Không được phép khóa tài khoản Chủ quán!");
        }

        staff.setActive(!staff.isActive());
        return userRepository.save(staff);
    }
}