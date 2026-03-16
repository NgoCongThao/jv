package ngo.cong.thao.s2o_pro.security.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import ngo.cong.thao.s2o_pro.common.response.ApiResponse;
import ngo.cong.thao.s2o_pro.security.JwtUtils;
import ngo.cong.thao.s2o_pro.user.entity.Role;
import ngo.cong.thao.s2o_pro.user.entity.User;
import ngo.cong.thao.s2o_pro.user.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/customer/auth")
@RequiredArgsConstructor
public class CustomerAuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> registerCustomer(@RequestBody CustomerRegisterReq req) {
        // Kiểm tra xem SĐT này đã dùng trên toàn hệ thống S2O chưa
        Optional<User> existingUser = userRepository.findByUsername(req.getPhone());
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("Số điện thoại này đã được đăng ký trên hệ thống!");
        }

        // Tạo tài khoản NỀN TẢNG (tenantId = null)
        User customer = User.builder()
                .username(req.getPhone())
                .password(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName())
                .role(Role.CUSTOMER) // Khách hàng App S2O
                .tenantId(null) // CỦA CHUNG NỀN TẢNG
                .isActive(true)
                .build();

        userRepository.save(customer);
        return ResponseEntity.ok(ApiResponse.success("Đăng ký tài khoản S2O thành công!"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> loginCustomer(@RequestBody CustomerLoginReq req) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getPhone(), req.getPassword())
        );

        User user = userRepository.findByUsername(req.getPhone()).orElseThrow();
        // Đổi từ: String token = jwtUtils.generateToken(user);
        // SỬA THÀNH:
        String token = jwtUtils.generateToken(user.getUsername(), user.getRole().name(), user.getTenantId());

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "token", token,
                "fullName", user.getFullName(),
                "role", user.getRole()
        )));
    }
}

@Data
class CustomerRegisterReq {
    private String phone;
    private String fullName;
    private String password;
}

@Data
class CustomerLoginReq {
    private String phone;
    private String password;
}