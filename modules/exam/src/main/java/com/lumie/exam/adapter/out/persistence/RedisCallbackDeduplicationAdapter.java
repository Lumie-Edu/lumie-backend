package com.lumie.exam.adapter.out.persistence;

import com.lumie.exam.application.port.out.CallbackDeduplicationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class RedisCallbackDeduplicationAdapter implements CallbackDeduplicationPort {

    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redis;

    @Override
    public boolean markIfAbsent(String key) {
        Boolean absent = redis.opsForValue().setIfAbsent(key, "1", TTL);
        return Boolean.TRUE.equals(absent);
    }

    @Override
    public void forget(String key) {
        redis.delete(key);
    }
}
