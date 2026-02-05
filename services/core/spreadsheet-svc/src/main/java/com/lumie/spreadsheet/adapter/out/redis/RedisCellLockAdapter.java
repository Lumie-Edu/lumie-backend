package com.lumie.spreadsheet.adapter.out.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.spreadsheet.application.port.out.CellLockPort;
import com.lumie.spreadsheet.domain.vo.CellLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisCellLockAdapter implements CellLockPort {

    private static final String LOCK_PREFIX = "spreadsheet:lock:";
    private static final String USER_LOCKS_PREFIX = "spreadsheet:user-locks:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<CellLock> tryAcquireLock(String spreadsheetId, String cellAddress,
                                              String userId, String userName, String userColor) {
        String lockKey = buildLockKey(spreadsheetId, cellAddress);

        String existingLock = redisTemplate.opsForValue().get(lockKey);
        if (existingLock != null) {
            try {
                CellLock existing = objectMapper.readValue(existingLock, CellLock.class);
                if (existing.userId().equals(userId)) {
                    CellLock refreshed = existing.refresh();
                    redisTemplate.opsForValue().set(lockKey, objectMapper.writeValueAsString(refreshed), LOCK_TTL);
                    return Optional.of(refreshed);
                }
                return Optional.empty();
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse existing lock: {}", e.getMessage());
            }
        }

        CellLock lock = CellLock.create(spreadsheetId, cellAddress, userId, userName, userColor);

        try {
            String lockValue = objectMapper.writeValueAsString(lock);
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, LOCK_TTL);

            if (Boolean.TRUE.equals(acquired)) {
                String userLocksKey = buildUserLocksKey(userId);
                redisTemplate.opsForSet().add(userLocksKey, lockKey);
                redisTemplate.expire(userLocksKey, Duration.ofMinutes(30));

                log.debug("Lock acquired: spreadsheet={}, cell={}, user={}", spreadsheetId, cellAddress, userId);
                return Optional.of(lock);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize lock: {}", e.getMessage());
        }

        return Optional.empty();
    }

    @Override
    public void releaseLock(String spreadsheetId, String cellAddress, String userId) {
        String lockKey = buildLockKey(spreadsheetId, cellAddress);
        String existingLock = redisTemplate.opsForValue().get(lockKey);

        if (existingLock != null) {
            try {
                CellLock lock = objectMapper.readValue(existingLock, CellLock.class);
                if (lock.userId().equals(userId)) {
                    redisTemplate.delete(lockKey);
                    redisTemplate.opsForSet().remove(buildUserLocksKey(userId), lockKey);
                    log.debug("Lock released: spreadsheet={}, cell={}, user={}", spreadsheetId, cellAddress, userId);
                }
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse lock for release: {}", e.getMessage());
            }
        }
    }

    @Override
    public void releaseAllLocksForUser(String userId) {
        String userLocksKey = buildUserLocksKey(userId);
        Set<String> lockKeys = redisTemplate.opsForSet().members(userLocksKey);

        if (lockKeys != null && !lockKeys.isEmpty()) {
            for (String lockKey : lockKeys) {
                String existingLock = redisTemplate.opsForValue().get(lockKey);
                if (existingLock != null) {
                    try {
                        CellLock lock = objectMapper.readValue(existingLock, CellLock.class);
                        if (lock.userId().equals(userId)) {
                            redisTemplate.delete(lockKey);
                        }
                    } catch (JsonProcessingException e) {
                        log.warn("Failed to parse lock: {}", e.getMessage());
                    }
                }
            }
            redisTemplate.delete(userLocksKey);
            log.info("Released all locks for user: {}", userId);
        }
    }

    @Override
    public void refreshLock(String spreadsheetId, String cellAddress, String userId) {
        String lockKey = buildLockKey(spreadsheetId, cellAddress);
        String existingLock = redisTemplate.opsForValue().get(lockKey);

        if (existingLock != null) {
            try {
                CellLock lock = objectMapper.readValue(existingLock, CellLock.class);
                if (lock.userId().equals(userId)) {
                    CellLock refreshed = lock.refresh();
                    redisTemplate.opsForValue().set(lockKey, objectMapper.writeValueAsString(refreshed), LOCK_TTL);
                    log.debug("Lock refreshed: spreadsheet={}, cell={}, user={}", spreadsheetId, cellAddress, userId);
                }
            } catch (JsonProcessingException e) {
                log.warn("Failed to refresh lock: {}", e.getMessage());
            }
        }
    }

    @Override
    public List<CellLock> getLocksForSpreadsheet(String spreadsheetId) {
        String pattern = LOCK_PREFIX + spreadsheetId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        return keys.stream()
                .map(key -> redisTemplate.opsForValue().get(key))
                .filter(Objects::nonNull)
                .map(value -> {
                    try {
                        return objectMapper.readValue(value, CellLock.class);
                    } catch (JsonProcessingException e) {
                        log.warn("Failed to parse lock: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<CellLock> getLock(String spreadsheetId, String cellAddress) {
        String lockKey = buildLockKey(spreadsheetId, cellAddress);
        String lockValue = redisTemplate.opsForValue().get(lockKey);

        if (lockValue == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(lockValue, CellLock.class));
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse lock: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private String buildLockKey(String spreadsheetId, String cellAddress) {
        return LOCK_PREFIX + spreadsheetId + ":" + cellAddress;
    }

    private String buildUserLocksKey(String userId) {
        return USER_LOCKS_PREFIX + userId;
    }
}
