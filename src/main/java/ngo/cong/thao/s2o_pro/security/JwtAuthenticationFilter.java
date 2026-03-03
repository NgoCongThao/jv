package ngo.cong.thao.s2o_pro.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ngo.cong.thao.s2o_pro.tenant.TenantContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final TokenBlacklistService tokenBlacklistService;

    // SỬA LỖI: Đã thêm TokenBlacklistService vào constructor
    public JwtAuthenticationFilter(JwtUtils jwtUtils, TokenBlacklistService tokenBlacklistService) {
        this.jwtUtils = jwtUtils;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = parseJwt(request);

            // 1. Kiểm tra token có hợp lệ về chữ ký và thời gian không
            if (jwt != null && jwtUtils.validateToken(jwt)) {

                // --- 2. CHỐT CHẶN REDIS BLACKLIST CỰC KỲ QUAN TRỌNG ---
                if (tokenBlacklistService.isBlacklisted(jwt)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"status\": 401, \"message\": \"Token đã bị thu hồi do người dùng đăng xuất.\"}");
                    return; // Ngắt luồng ngay lập tức, hất văng request ra ngoài
                }
                // --------------------------------------------------------

                // 3. Nếu token an toàn, tiến hành trích xuất dữ liệu
                Claims claims = jwtUtils.getClaimsFromToken(jwt);

                String username = claims.getSubject();
                String role = claims.get("role", String.class);
                String tenantId = claims.get("tenantId", String.class);

                // Lưu quyền vào Spring Security Context
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // Bơm tenant_id vào TenantContext
                TenantContext.setTenantId(tenantId);
            }

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            // Có thể thêm log lỗi ở đây nếu cần thiết
            logger.error("Không thể thiết lập xác thực người dùng: {}", e);
        } finally {
            // Bắt buộc phải dọn dẹp sau khi request xử lý xong để tránh rò rỉ dữ liệu giữa các luồng
            TenantContext.clear();
        }
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}