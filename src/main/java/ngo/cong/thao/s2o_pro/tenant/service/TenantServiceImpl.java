package ngo.cong.thao.s2o_pro.tenant.service;

import lombok.RequiredArgsConstructor;
import ngo.cong.thao.s2o_pro.tenant.dto.TenantRegisterRequest;
import ngo.cong.thao.s2o_pro.tenant.repository.TenantRepository;
import ngo.cong.thao.s2o_pro.user.entity.Role;
import ngo.cong.thao.s2o_pro.user.entity.User;
import ngo.cong.thao.s2o_pro.user.repository.UserRepository;

// CHÚ Ý: Import đúng đường dẫn của Tenant và TenantStatus (tùy theo anh đang để nó ở thư mục user hay tenant)
import ngo.cong.thao.s2o_pro.user.entity.Tenant;
import ngo.cong.thao.s2o_pro.tenant.entity.TenantStatus;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Tenant registerTenant(TenantRegisterRequest request) {
        // Kiểm tra domain đã tồn tại chưa
        if (tenantRepository.findByDomain(request.getDomain()).isPresent()) {
            throw new IllegalArgumentException("Tên miền (Domain) này đã có người đăng ký!");
        }

        // Tạo mới Tenant với trạng thái PENDING (Đã xóa dòng .isActive)
        Tenant newTenant = Tenant.builder()
                .name(request.getRestaurantName())
                .domain(request.getDomain())
                .address(request.getAddress())
                .phone(request.getPhone())
                .ownerEmail(request.getOwnerEmail())
                .status(TenantStatus.PENDING) // SỬ DỤNG ENUM CHUẨN XỊN
                .build();

        return tenantRepository.save(newTenant);
    }

    @Override
    @Transactional
    public String approveTenant(String tenantId) {
        Tenant tenant = tenantRepository.findById(UUID.fromString(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Nhà hàng này"));

        // SO SÁNH BẰNG ENUM
        if (tenant.getStatus() == TenantStatus.ACTIVE) {
            throw new IllegalArgumentException("Nhà hàng này đã được duyệt từ trước rồi!");
        }

        // 1. Cập nhật trạng thái nhà hàng thành ACTIVE (Đã xóa dòng .setActive)
        tenant.setStatus(TenantStatus.ACTIVE);
        tenantRepository.save(tenant);

        // 2. Tạo tài khoản OWNER cho chủ quán với mật khẩu ngẫu nhiên (8 ký tự)
        String randomPassword = UUID.randomUUID().toString().substring(0, 8);
        String username = "owner_" + tenant.getDomain(); // Ví dụ: owner_bunbohue

        User owner = User.builder()
                .username(username)
                .password(passwordEncoder.encode(randomPassword))
                .fullName("Chủ quán " + tenant.getName())
                .role(Role.OWNER) // Phân quyền cao nhất của Tenant
                .tenantId(tenant.getDomain())
                .isActive(true) // User thì vẫn có isActive bình thường anh nhé
                .build();

        userRepository.save(owner);

        // 3. Ở thực tế, chỗ này anh sẽ gọi EmailService để gửi Mail cho khách.
        // Tạm thời mình in ra Console và trả về chuỗi để anh test trên Postman nhé.
        String message = String.format("✅ Đã duyệt thành công! Tài khoản: %s | Mật khẩu: %s", username, randomPassword);
        System.out.println("📧 [MÔ PHỎNG GỬI EMAIL] " + message + " -> Gửi tới: " + tenant.getOwnerEmail());

        return message;
    }
}