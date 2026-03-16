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

    public JwtAuthenticationFilter(JwtUtils jwtUtils, TokenBlacklistService tokenBlacklistService) {
        this.jwtUtils = jwtUtils;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // LẤY SẴN TENANT ID TỪ HEADER (Phục vụ cho Khách nền tảng và Khách vãng lai)
            String tenantIdFromHeader = request.getHeader("X-Tenant-ID");

            String jwt = parseJwt(request);

            // NẾU CÓ TOKEN (Đăng nhập bằng tài khoản)
            if (jwt != null && jwtUtils.validateToken(jwt)) {

                // Chốt chặn Blacklist
                if (tokenBlacklistService.isBlacklisted(jwt)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"status\": 401, \"message\": \"Token đã bị thu hồi.\"}");
                    return;
                }

                Claims claims = jwtUtils.getClaimsFromToken(jwt);
                String username = claims.getSubject();
                String role = claims.get("role", String.class);
                String tenantIdFromJwt = claims.get("tenantId", String.class);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // ---> ĐÂY LÀ ĐOẠN MA THUẬT QUYẾT ĐỊNH <---
                // Ưu tiên TenantId từ JWT (Nhân viên/Chủ quán).
                // Nếu JWT không có (Khách App S2O), thì lấy từ Header truyền lên.
                String finalTenantId = (tenantIdFromJwt != null) ? tenantIdFromJwt : tenantIdFromHeader;
                TenantContext.setTenantId(finalTenantId);

            } else {
                // NẾU KHÔNG CÓ TOKEN (Khách vãng lai - GUEST quét QR)
                // Vẫn phải nạp TenantId từ Header vào Context để họ xem Menu và Đặt món được
                if (tenantIdFromHeader != null) {
                    TenantContext.setTenantId(tenantIdFromHeader);
                }
            }

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            logger.error("Không thể thiết lập xác thực người dùng: {}", e);
        } finally {
            // Bắt buộc dọn dẹp để tránh rò rỉ dữ liệu
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