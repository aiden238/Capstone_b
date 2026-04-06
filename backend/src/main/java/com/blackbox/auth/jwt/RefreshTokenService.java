package com.blackbox.auth.jwt;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh:";
    private static final String BLACKLIST_PREFIX = "blacklist:";
    private final StringRedisTemplate redisTemplate;

    public RefreshTokenService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(UUID userId, String refreshToken, long ttlMillis) {
        redisTemplate.opsForValue().set(
                KEY_PREFIX + userId,
                refreshToken,
                ttlMillis,
                TimeUnit.MILLISECONDS
        );
    }

    public Optional<String> find(UUID userId) {
        String token = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
        return Optional.ofNullable(token);
    }

    public void delete(UUID userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }

    public void blacklistAccessToken(String token, long ttlMillis) {
        if (ttlMillis > 0) {
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + token,
                    "logout",
                    ttlMillis,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }
}
