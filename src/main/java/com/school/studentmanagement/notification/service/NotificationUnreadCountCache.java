package com.school.studentmanagement.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.LongSupplier;

/**
 * 미확인 알림 개수를 Redis에 캐시한다(cache-aside).
 * 폴링이 매번 DB COUNT를 때리지 않도록 하고, 알림 생성/확인 시 evict하여 드리프트를 막는다.
 * (TTL을 함께 둬서 evict 누락 시에도 일정 시간 내 자동 정합)
 */
@Component
@RequiredArgsConstructor
public class NotificationUnreadCountCache {

    private static final String KEY_PREFIX = "notif:unread:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;

    // 캐시 hit이면 그대로, miss면 loader로 계산 후 채워 넣는다.
    public long getOrLoad(Long userId, LongSupplier loader) {
        String key = key(userId);
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            try {
                return Long.parseLong(cached);
            } catch (NumberFormatException ignored) {
                // 손상된 값은 무시하고 재계산
            }
        }
        long value = loader.getAsLong();
        redisTemplate.opsForValue().set(key, Long.toString(value), TTL);
        return value;
    }

    public void evict(Long userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
