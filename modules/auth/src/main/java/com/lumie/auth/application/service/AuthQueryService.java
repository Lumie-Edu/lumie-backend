package com.lumie.auth.application.service;

import com.lumie.auth.application.dto.response.SessionResponse;
import com.lumie.auth.application.dto.response.UserResponse;
import com.lumie.auth.application.port.out.SessionPersistencePort;
import com.lumie.auth.application.port.out.UserLookupPort;
import com.lumie.auth.application.port.out.UserLookupPort.UserData;
import com.lumie.auth.domain.vo.AuthSession;
import com.lumie.common.exception.ResourceNotFoundException;
import com.lumie.tenant.api.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthQueryService {

    private final SessionPersistencePort sessionPersistencePort;
    private final UserLookupPort userLookupPort;
    private final TenantService tenantService;

    public Optional<AuthSession> findActiveSession(String sessionId) {
        return sessionPersistencePort.findById(sessionId)
                .filter(session -> {
                    if (session.isExpired(Instant.now())) {
                        sessionPersistencePort.delete(session);
                        return false;
                    }
                    return true;
                });
    }

    public UserResponse getMe(Long userId, String tenantSlug) {
        UserData user = userLookupPort.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", String.valueOf(userId)));
        TenantService.TenantData tenant = tenantService.getTenantBySlug(tenantSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantSlug));
        return user.toUserResponse(tenantSlug, tenant.onboardingCompletedAt());
    }

    public List<SessionResponse> getSessions(Long userId, String tenantSlug, String currentSessionId) {
        Set<String> sessionIds = sessionPersistencePort.findSessionIds(tenantSlug, userId);
        return sessionPersistencePort.findSessions(tenantSlug, userId, sessionIds).values().stream()
                .map(session -> new SessionResponse(
                        session.id(),
                        session.device(),
                        session.deviceCategory().name(),
                        session.createdAt().toEpochMilli(),
                        session.id().equals(currentSessionId)
                ))
                .sorted(Comparator.comparingLong(SessionResponse::createdAt).reversed())
                .toList();
    }
}
