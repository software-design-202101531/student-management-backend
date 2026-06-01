package com.school.studentmanagement.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 사용자별 활성 리프레시 토큰을 Redis에 저장/대조/삭제한다.
 * 사용자당 1개만 유지하므로, 로그인/회전 시 갱신되고 그 이전 토큰은 자동으로 무효가 된다.
 * (회전 + 진짜 로그아웃/강제 무효화를 위해 상태를 둔다)
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "auth:refresh:";

    private final StringRedisTemplate redisTemplate;

    // 저장(또는 갱신). ttlMillis 동안만 유효.
    public void save(Long userId, String refreshToken, long ttlMillis) {
        redisTemplate.opsForValue().set(key(userId), refreshToken, Duration.ofMillis(ttlMillis));
    }

    // 제시된 토큰이 현재 저장된(=가장 최근에 발급된) 토큰과 일치하는지
    public boolean matches(Long userId, String refreshToken) {
        String stored = redisTemplate.opsForValue().get(key(userId));
        return stored != null && stored.equals(refreshToken);
    }

    // 무효화 (로그아웃 / 재사용 의심 시)
    public void delete(Long userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
