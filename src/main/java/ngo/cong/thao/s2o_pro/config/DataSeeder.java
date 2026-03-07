package ngo.cong.thao.s2o_pro.config;

import ngo.cong.thao.s2o_pro.tenant.entity.Tenant;
import ngo.cong.thao.s2o_pro.tenant.repository.TenantRepository;
import ngo.cong.thao.s2o_pro.user.entity.Role;
import ngo.cong.thao.s2o_pro.user.entity.User;
import ngo.cong.thao.s2o_pro.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, TenantRepository tenantRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Kiểm tra xem admin đã tồn tại chưa để tránh tạo trùng lặp mỗi khi restart app
        if (userRepository.findByUsername("admin").isEmpty()) {

            // 1. Tạo tài khoản Platform Admin (Quản trị viên hệ thống S2O)
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("123456")) // Mã hóa mật khẩu
                    .fullName("Quản trị viên Hệ thống")
                    .role(Role.ADMIN)
                    .tenantId(null) // Admin không thuộc nhà hàng nào
                    .isActive(true)
                    .build();
            userRepository.save(admin);
            System.out.println("✅ Đã tạo tài khoản PLATFORM_ADMIN: admin / 123456");

            // 2. Tạo một Nhà hàng mẫu
            Tenant tenant = Tenant.builder()
                    .name("Nhà hàng Biển Đông")
                    .domain("biendong")
                    .address("Quận 1, TP.HCM")
                    .phone("0123456789")
                    .isActive(true)
                    .build();
            tenant = tenantRepository.save(tenant);
            System.out.println("✅ Đã tạo nhà hàng mẫu: " + tenant.getName() + " (ID: " + tenant.getId() + ")");

            // 3. Tạo tài khoản Chủ nhà hàng (Thuộc nhà hàng Biển Đông)
            User owner = User.builder()
                    .username("owner_biendong")
                    .password(passwordEncoder.encode("123456")) // Mã hóa mật khẩu
                    .fullName("Chủ nhà hàng Biển Đông")
                    .role(Role.OWNER)
                    .tenantId(tenant.getId().toString()) // Gắn ID của nhà hàng vào user này
                    .isActive(true)
                    .build();
            userRepository.save(owner);
            System.out.println("✅ Đã tạo tài khoản RESTAURANT_OWNER: owner_biendong / 123456");
        }
    }
}