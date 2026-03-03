package ngo.cong.thao.s2o_pro.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@EnableCaching // Bật tính năng Cache toàn cục
public class RedisConfig {

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        // 1. Tạo một ObjectMapper tùy chỉnh
        ObjectMapper objectMapper = new ObjectMapper();

        // 2. Đăng ký module JSR-310 để xử lý LocalDateTime
        objectMapper.registerModule(new JavaTimeModule());

        // 3. Tắt tính năng lưu ngày tháng dưới dạng Timestamp (số), giữ nguyên dạng chuỗi dễ đọc
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 4. Kích hoạt tính năng lưu kèm thông tin class để khi lấy từ Redis ra, Spring biết ép kiểu về class nào
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        // 5. Truyền bộ chuyển đổi tùy chỉnh này vào Redis
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(60)) // Cache tồn tại 60 phút
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
    }
}