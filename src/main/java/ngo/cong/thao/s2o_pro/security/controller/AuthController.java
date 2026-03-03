package ngo.cong.thao.s2o_pro.security.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import ngo.cong.thao.s2o_pro.common.response.ApiResponse;
import ngo.cong.thao.s2o_pro.security.JwtUtils;
import ngo.cong.thao.s2o_pro.security.TokenBlacklistService;
import ngo.cong.thao.s2o_pro.security.dto.AuthResponse;
import ngo.cong.thao.s2o_pro.security.dto.LoginRequest;
import ngo.cong.thao.s2o_pro.user.entity.User;
import ngo.cong.thao.s2o_pro.user.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final TokenBlacklistService tokenBlacklistService;

    // Đã thêm TokenBlacklistService vào Constructor để Spring tự động Inject
    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtils jwtUtils,
                          UserRepository userRepository,
                          TokenBlacklistService tokenBlacklistService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        // 1. Xác thực username và password
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        // 2. Lưu trạng thái xác thực vào Context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. Lấy thông tin user từ DB
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        // 4. Tạo JWT Token
        String token = jwtUtils.generateToken(user.getUsername(), user.getRole().name(), user.getTenantId());

        // 5. Trả về kết quả
        AuthResponse authResponse = AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole().name())
                .tenantId(user.getTenantId())
                .build();

        return ResponseEntity.ok(ApiResponse.success(authResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);

            // Tính thời gian sống còn lại của Token
            long expirationTime = jwtUtils.getExpirationTime(jwt);
            long remainingTime = expirationTime - System.currentTimeMillis();

            // Nếu token chưa tự hết hạn thì tống nó vào Redis Blacklist
            if (remainingTime > 0) {
                tokenBlacklistService.addToBlacklist(jwt, remainingTime);
            }
        }
        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công, Token đã bị thu hồi."));
    }
}