package ngo.cong.thao.s2o_pro.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username, String role, String tenantId) {
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .claim("tenantId", tenantId)
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    // Thêm hàm này để lấy chính xác thời gian (mili-giây) token sẽ hết hạn
    // Thêm hàm này để lấy chính xác thời gian (mili-giây) token sẽ hết hạn
    public long getExpirationTime(String token) {
        // Dùng luôn hàm getClaimsFromToken bạn đã viết sẵn ở trên
        return getClaimsFromToken(token).getExpiration().getTime();
    }
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            // Trong thực tế sẽ log lỗi: Hết hạn, sai chữ ký,...
            return false;
        }
    }
}