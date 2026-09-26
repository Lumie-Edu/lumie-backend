package com.lumie.auth.application.service;

import com.lumie.auth.adapter.out.config.SessionConfig;
import com.lumie.auth.application.port.out.SessionPersistencePort;
import com.lumie.auth.application.port.out.UserLookupPort.UserData;
import com.lumie.auth.domain.vo.AuthSession;
import com.lumie.auth.domain.vo.DeviceCategory;
import com.lumie.auth.domain.vo.SessionMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

/** Creates and enforces lifecycle rules for opaque browser sessions. */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginSessionHelper {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final SessionPersistencePort sessionPersistencePort;
    private final SessionConfig sessionConfig;

    public AuthSession create(UserData user, String tenantSlug, Long tenantId, SessionMetadata metadata) {
        DeviceCategory category = metadata.category() == null ? DeviceCategory.UNKNOWN : metadata.category();
        enforceSingleSessionPerDeviceCategory(tenantSlug, user.id(), category);

        Instant now = Instant.now();
        AuthSession session = new AuthSession(
                newSessionId(),
                user.id(),
                user.name(),
                tenantSlug,
                tenantId,
                user.role(),
                metadata.device() == null ? "Unknown" : metadata.device(),
                category,
                now,
                now.plusSeconds(sessionConfig.getExpiration())
        );
        sessionPersistencePort.save(session);
        return session;
    }

    private void enforceSingleSessionPerDeviceCategory(String tenantSlug, Long userId, DeviceCategory category) {
        if (category == DeviceCategory.UNKNOWN) {
            return;
        }
        Set<String> sessionIds = sessionPersistencePort.findSessionIds(tenantSlug, userId);
        Map<String, AuthSession> sessions = sessionPersistencePort.findSessions(tenantSlug, userId, sessionIds);
        for (AuthSession session : sessions.values()) {
            if (session.deviceCategory() == category) {
                sessionPersistencePort.delete(session);
                log.info("Replaced existing {} session for user {}", category, userId);
            }
        }
    }

    private String newSessionId() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
