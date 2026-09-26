package com.lumie.auth.application.service;

import com.lumie.auth.application.dto.request.LoginRequest;
import com.lumie.auth.application.dto.response.LoginResponse;
import com.lumie.auth.application.exception.AuthErrors.InvalidCredentialsException;
import com.lumie.auth.application.exception.AuthErrors.TenantInactiveException;
import com.lumie.auth.application.exception.AuthErrors.UserDisabledException;
import com.lumie.auth.application.port.out.SessionPersistencePort;
import com.lumie.auth.application.port.out.UserDirectoryPort;
import com.lumie.auth.application.port.out.UserLookupPort;
import com.lumie.auth.application.port.out.UserLookupPort.UserData;
import com.lumie.auth.domain.vo.AuthSession;
import com.lumie.auth.domain.vo.SessionMetadata;
import com.lumie.common.exception.ResourceNotFoundException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.tenant.api.TenantService;
import com.lumie.tenant.api.TenantService.TenantData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

/** Login, logout, and session revocation use cases backed by Redis. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthSessionService {

    private final TenantService tenantService;
    private final UserLookupPort userLookupPort;
    private final UserDirectoryPort userDirectoryPort;
    private final SessionPersistencePort sessionPersistencePort;
    private final PasswordEncoder passwordEncoder;
    private final LoginSessionHelper loginSessionHelper;

    public LoginResponse login(LoginRequest request, SessionMetadata metadata) {
        TenantData tenant = resolveLoginTenant(request);
        return TenantContextHolder.withinContext(tenant.slug(), tenant.id(), () -> {
            UserData user = userLookupPort.findByUserLoginId(request.userLoginId())
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid user login ID or password"));
            if (!user.enabled()) {
                throw new UserDisabledException("User account is disabled");
            }
            if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
                throw new InvalidCredentialsException("Invalid user login ID or password");
            }
            AuthSession session = loginSessionHelper.create(user, tenant.slug(), tenant.id(), metadata);
            return LoginResponse.of(session, user.toUserResponse(tenant.slug(), tenant.onboardingCompletedAt()));
        });
    }

    public void logout(String sessionId) {
        sessionPersistencePort.findById(sessionId).ifPresent(sessionPersistencePort::delete);
    }

    public void logoutAll(AuthSession session) {
        sessionPersistencePort.deleteAllForUser(session.tenantSlug(), session.userId());
    }

    public void revokeSession(Long userId, String tenantSlug, String sessionId) {
        AuthSession session = sessionPersistencePort.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));
        if (!session.userId().equals(userId) || !session.tenantSlug().equals(tenantSlug)) {
            throw new ResourceNotFoundException("Session", sessionId);
        }
        sessionPersistencePort.delete(session);
    }

    public void revokeOtherSessions(Long userId, String tenantSlug, String currentSessionId) {
        Set<String> sessionIds = sessionPersistencePort.findSessionIds(tenantSlug, userId);
        for (AuthSession session : sessionPersistencePort.findSessions(tenantSlug, userId, sessionIds).values()) {
            if (!session.id().equals(currentSessionId)) {
                sessionPersistencePort.delete(session);
            }
        }
    }

    private TenantData resolveLoginTenant(LoginRequest request) {
        String hostTenantSlug = TenantContextHolder.getTenant();
        if (hostTenantSlug != null && !hostTenantSlug.isBlank()) {
            TenantData tenant = tenantService.getTenantBySlug(hostTenantSlug)
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid user login ID or password"));
            if (!tenant.isActive()) {
                throw new TenantInactiveException("Tenant is not active");
            }
            return tenant;
        }

        UserDirectoryPort.DirectoryEntry entry = userDirectoryPort.findByUserLoginId(request.userLoginId())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid user login ID or password"));
        TenantData tenant = tenantService.getTenant(entry.tenantId())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid user login ID or password"));
        if (!tenant.isActive()) {
            throw new TenantInactiveException("Tenant is not active");
        }
        return tenant;
    }
}
