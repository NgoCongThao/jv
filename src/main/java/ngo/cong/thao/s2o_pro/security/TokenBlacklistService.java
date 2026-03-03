package ngo.cong.thao.s2o_pro.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    // Công cụ của Spring Boot để làm việc với Redis dạng chuỗi (String)
    private final StringRedisTemplate redisTemplate;

    // Tống token vào "Danh sách đen" (Blacklist) với thời gian sống (TTL)
    public void addToBlacklist(String token, long expirationInMilliSeconds) {
        redisTemplate.opsForValue().set("blacklist:" + token, "true", expirationInMilliSeconds, TimeUnit.MILLISECONDS);
    }

    // Kiểm tra xem token này có đang bị cấm cửa không
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token));
    }
}