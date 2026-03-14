package ngo.cong.thao.s2o_pro.config;

import ngo.cong.thao.s2o_pro.menu.entity.MenuCategory;
import ngo.cong.thao.s2o_pro.menu.entity.MenuItem;
import ngo.cong.thao.s2o_pro.menu.repository.MenuCategoryRepository;
import ngo.cong.thao.s2o_pro.menu.repository.MenuItemRepository;
import ngo.cong.thao.s2o_pro.tenant.entity.Tenant;
import ngo.cong.thao.s2o_pro.tenant.repository.TenantRepository;
import ngo.cong.thao.s2o_pro.user.entity.Role;
import ngo.cong.thao.s2o_pro.user.entity.User;
import ngo.cong.thao.s2o_pro.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      TenantRepository tenantRepository,
                      MenuCategoryRepository menuCategoryRepository,
                      MenuItemRepository menuItemRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.menuCategoryRepository = menuCategoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Chỉ chạy Seeder nếu chưa có tài khoản admin (tránh tạo trùng lặp khi restart)
        if (userRepository.findByUsername("admin").isEmpty()) {

            // 1. TẠO PLATFORM ADMIN (QUẢN TRỊ VIÊN HỆ THỐNG)
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("123456"))
                    .fullName("Quản trị viên S2O")
                    .role(Role.ADMIN)
                    .tenantId(null) // Admin không thuộc Tenant nào
                    .isActive(true)
                    .build();
            userRepository.save(admin);
            System.out.println("✅ Đã tạo tài khoản PLATFORM_ADMIN: admin / 123456");

            // ==========================================
            // NHÀ HÀNG 1: NHÀ HÀNG BIỂN ĐÔNG (Hải sản)
            // ==========================================
            Tenant t1 = tenantRepository.save(Tenant.builder()
                    .name("Nhà hàng Biển Đông")
                    .domain("biendong")
                    .address("Quận 1, TP.HCM")
                    .phone("0901111111")
                    .isActive(true)
                    .build());

            userRepository.save(User.builder()
                    .username("owner_biendong")
                    .password(passwordEncoder.encode("123456"))
                    .fullName("Chủ nhà hàng Biển Đông")
                    .role(Role.OWNER)
                    .tenantId(t1.getId().toString())
                    .isActive(true)
                    .build());

            seedMenuForTenant(t1.getId().toString(), "Hải Sản Tươi Sống", "Đồ Uống & Tráng Miệng", List.of(
                    createItem("Cua Hoàng Đế Hấp", 1500000, "Cua tươi sống 1kg bắt tại hồ"),
                    createItem("Tôm Hùm Nướng Bơ Tỏi", 850000, "Tôm hùm bông loại 1 nướng phô mai bơ tỏi"),
                    createItem("Mực Ống Chiên Giòn", 150000, "Mực ống loại lớn chiên giòn rụm"),
                    createItem("Hàu Né Phô Mai", 120000, "Hàu sữa nướng phô mai béo ngậy")
            ), List.of(
                    createItem("Bia Heineken", 25000, "Bia lon 330ml ướp lạnh"),
                    createItem("Nước Ép Dưa Hấu", 40000, "Nước ép nguyên chất 100%"),
                    createItem("Chè Khúc Bạch", 35000, "Tráng miệng thanh mát")
            ));

            // ==========================================
            // NHÀ HÀNG 2: KATINAT SAIGON KAFE
            // ==========================================
            Tenant t2 = tenantRepository.save(Tenant.builder()
                    .name("Katinat Saigon Kafe")
                    .domain("katinat")
                    .address("Quận 3, TP.HCM")
                    .phone("0902222222")
                    .isActive(true)
                    .build());

            userRepository.save(User.builder()
                    .username("owner_katinat")
                    .password(passwordEncoder.encode("123456"))
                    .fullName("Chủ quán Katinat")
                    .role(Role.OWNER)
                    .tenantId(t2.getId().toString())
                    .isActive(true)
                    .build());

            seedMenuForTenant(t2.getId().toString(), "Cà Phê Truyền Thống", "Trà Trái Cây", List.of(
                    createItem("Bạc Xỉu", 45000, "Cà phê sữa nhiều sữa đá"),
                    createItem("Cà Phê Sữa Đá", 39000, "Cà phê pha phin truyền thống đậm đà"),
                    createItem("Americano Đá", 45000, "Cà phê đen nguyên chất Espresso")
            ), List.of(
                    createItem("Trà Đào Cam Sả", 55000, "Best seller thanh mát giải nhiệt"),
                    createItem("Trà Vải Oolong", 55000, "Trà vải ngọt thanh đậm vị Oolong"),
                    createItem("Trà Sữa Oolong Nướng", 60000, "Trà sữa đậm vị trà nướng thơm lừng")
            ));

            // ==========================================
            // NHÀ HÀNG 3: GOGI HOUSE (Thịt Nướng BBQ)
            // ==========================================
            Tenant t3 = tenantRepository.save(Tenant.builder()
                    .name("GoGi House - Thịt Nướng Hàn Quốc")
                    .domain("gogi")
                    .address("Quận 10, TP.HCM")
                    .phone("0903333333")
                    .isActive(true)
                    .build());

            userRepository.save(User.builder()
                    .username("owner_gogi")
                    .password(passwordEncoder.encode("123456"))
                    .fullName("Quản lý Gogi House")
                    .role(Role.OWNER)
                    .tenantId(t3.getId().toString())
                    .isActive(true)
                    .build());

            seedMenuForTenant(t3.getId().toString(), "Thịt Bò Mỹ Nướng", "Món Ăn Kèm Truyền Thống", List.of(
                    createItem("Dẻ Sườn Bò Mỹ", 250000, "Dẻ sườn ướp sốt Galbi đặc biệt"),
                    createItem("Ba Chỉ Bò Mỹ Đảo Đá", 180000, "Ba chỉ mềm mọng nước thái lát mỏng"),
                    createItem("Lõi Vai Bò Mỹ Mềm", 280000, "Thịt lõi vai nướng tảng cực mềm")
            ), List.of(
                    createItem("Cơm Trộn Bát Đá", 85000, "Cơm trộn Bibimbap truyền thống Hàn Quốc"),
                    createItem("Canh Kim Chi Thịt Heo", 75000, "Canh kim chi chua cay nồng ấm"),
                    createItem("Mì Lạnh Naengmyeon", 80000, "Mì lạnh thanh mát giải ngấy thịt nướng")
            ));

            System.out.println("✅ Đã khởi tạo thành công dữ liệu mẫu cho 3 NHÀ HÀNG (Tenants) cùng thực đơn đầy đủ!");
        }
    }

    // --- CÁC HÀM HỖ TRỢ TẠO NHANH DỮ LIỆU MENU ---

    private MenuItem createItem(String name, int price, String desc) {
        return MenuItem.builder()
                .name(name)
                .price(BigDecimal.valueOf(price))
                .description(desc)
                .isAvailableDineIn(true)
                .isAvailableDelivery(true) // Có thể đổi thành false với món không cho mang về
                .isActive(true)
                .mealTime("ALL") // Phục vụ cả ngày
                .build();
    }

    private void seedMenuForTenant(String tenantId, String cat1Name, String cat2Name, List<MenuItem> items1, List<MenuItem> items2) {
        // Lưu Danh mục 1
        MenuCategory cat1 = MenuCategory.builder()
                .name(cat1Name)
                .description("Danh mục " + cat1Name)
                .isActive(true)
                .build();
        cat1.setTenantId(tenantId);
        cat1 = menuCategoryRepository.save(cat1);

        // Lưu các món thuộc Danh mục 1
        for (MenuItem item : items1) {
            item.setCategory(cat1);
            item.setTenantId(tenantId);
            menuItemRepository.save(item);
        }

        // Lưu Danh mục 2
        MenuCategory cat2 = MenuCategory.builder()
                .name(cat2Name)
                .description("Danh mục " + cat2Name)
                .isActive(true)
                .build();
        cat2.setTenantId(tenantId);
        cat2 = menuCategoryRepository.save(cat2);

        // Lưu các món thuộc Danh mục 2
        for (MenuItem item : items2) {
            item.setCategory(cat2);
            item.setTenantId(tenantId);
            menuItemRepository.save(item);
        }
    }
}