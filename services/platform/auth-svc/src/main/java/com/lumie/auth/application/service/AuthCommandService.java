package com.lumie.auth.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.auth.application.dto.request.ChangePasswordRequest;
import com.lumie.auth.application.dto.request.LoginRequest;
import com.lumie.auth.application.dto.request.OwnerRegisterRequest;
import com.lumie.auth.application.dto.request.RefreshTokenRequest;
import com.lumie.auth.application.dto.request.RegisterRequest;
import com.lumie.auth.application.dto.request.UpdateProfileRequest;
import com.lumie.auth.application.dto.response.LoginResponse;
import com.lumie.auth.application.dto.response.TokenResponse;
import com.lumie.auth.application.dto.response.UserResponse;
import com.lumie.auth.application.port.in.ChangePasswordUseCase;
import com.lumie.auth.application.port.in.LoginUseCase;
import com.lumie.auth.application.port.in.LogoutUseCase;
import com.lumie.auth.application.port.in.OwnerRegisterUseCase;
import com.lumie.auth.application.port.in.RefreshTokenUseCase;
import com.lumie.auth.application.port.in.RegisterUseCase;
import com.lumie.auth.application.port.in.RevokeSessionUseCase;
import com.lumie.auth.application.port.in.UpdateProfileUseCase;
import com.lumie.auth.domain.vo.DeviceCategory;
import com.lumie.common.auth.Role;
import com.lumie.auth.domain.vo.SessionMetadata;
import com.lumie.auth.application.port.out.AdminServicePort;
import com.lumie.auth.application.port.out.TenantServicePort;
import com.lumie.auth.application.port.out.TenantServicePort.TenantCreationResult;
import com.lumie.auth.application.port.out.TenantServicePort.TenantData;
import com.lumie.auth.application.port.out.TokenPersistencePort;
import com.lumie.auth.application.port.out.UserDirectoryPort;
import com.lumie.auth.application.port.out.UserLookupPort;
import com.lumie.auth.application.port.out.UserLookupPort.UserData;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.auth.domain.entity.AuthToken;
import com.lumie.auth.domain.vo.TokenClaims;
import com.lumie.auth.domain.vo.TokenPair;
import com.lumie.auth.infrastructure.security.JwtTokenProvider;
import com.lumie.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthCommandService implements LoginUseCase, LogoutUseCase, RefreshTokenUseCase,
        RegisterUseCase, OwnerRegisterUseCase, UpdateProfileUseCase, ChangePasswordUseCase,
        RevokeSessionUseCase {

    private final TenantServicePort tenantServicePort;
    private final UserLookupPort userLookupPort;
    private final UserDirectoryPort userDirectoryPort;
    private final AdminServicePort adminServicePort;
    private final TokenPersistencePort tokenPersistencePort;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Override
    public LoginResponse register(RegisterRequest request, SessionMetadata metadata) {
        log.info("Registration attempt for userLoginId: {} on tenant: {}", request.userLoginId(), request.tenantSlug());

        TenantData tenant = tenantServicePort.validateTenant(request.tenantSlug())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", request.tenantSlug()));

        if (!tenant.isActive()) {
            throw new IllegalStateException("Tenant is not active: " + request.tenantSlug());
        }

        return withinTenantContext(tenant.slug(), tenant.id(), () -> {
            if (userLookupPort.existsByUserLoginId(request.userLoginId())) {
                throw new UserLoginIdAlreadyExistsException("User login ID already registered: " + request.userLoginId());
            }

            String passwordHash = passwordEncoder.encode(request.password());
            UserData user = userLookupPort.createUser(
                    request.userLoginId(),
                    request.name(),
                    request.phone(),
                    passwordHash,
                    Role.STUDENT
            );

            IssuedTokens issued = issueTokensForLogin(user, request.tenantSlug(), tenant.id(), metadata);

            log.info("Registration successful for user: {} on tenant: {}", user.id(), request.tenantSlug());

            return LoginResponse.of(issued.tokenPair(), user.toUserResponse(request.tenantSlug()));
        });
    }

    @Override
    public LoginResponse login(LoginRequest request, SessionMetadata metadata) {
        log.info("Login attempt for userLoginId: {}", request.userLoginId());

        // Resolve which tenant this login targets:
        //   - Portal entry (lumie-edu.com/{customId}): RequestContextFilter has
        //     already set TenantContextHolder from the X-Tenant-Slug header.
        //   - Root entry (lumie-edu.com): no header, must look up the OWNER
        //     directory in public.users to discover the tenant.
        TenantData tenant = resolveLoginTenant(request);

        return withinTenantContext(tenant.slug(), tenant.id(), () -> {
            UserData user = userLookupPort.findByUserLoginId(request.userLoginId())
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid user login ID or password"));

            if (!user.enabled()) {
                throw new IllegalStateException("User account is disabled");
            }

            if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
                throw new InvalidCredentialsException("Invalid user login ID or password");
            }

            IssuedTokens issued = issueTokensForLogin(user, tenant.slug(), tenant.id(), metadata);

            log.info("Login successful for user: {} on tenant: {}", user.id(), tenant.slug());

            return LoginResponse.of(issued.tokenPair(), user.toUserResponse(tenant.slug()));
        });
    }

    private TenantData resolveLoginTenant(LoginRequest request) {
        String headerSlug = TenantContextHolder.getTenant();
        if (headerSlug != null && !headerSlug.isBlank()) {
            // Portal entry — header provides the tenant.
            TenantData tenant = tenantServicePort.validateTenant(headerSlug)
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid user login ID or password"));
            if (!tenant.isActive()) {
                throw new IllegalStateException("Tenant is not active");
            }
            return tenant;
        }

        // Root entry — directory lookup. Must be an OWNER (only OWNERs are in
        // public.users); otherwise the login is rejected with the standard
        // generic error so we don't leak which login_ids exist.
        UserDirectoryPort.DirectoryEntry entry = userDirectoryPort.findByUserLoginId(request.userLoginId())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid user login ID or password"));

        TenantData tenant = tenantServicePort.validateTenantById(entry.tenantId())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid user login ID or password"));
        if (!tenant.isActive()) {
            throw new IllegalStateException("Tenant is not active");
        }
        return tenant;
    }

    @Override
    public LoginResponse registerOwner(OwnerRegisterRequest request, SessionMetadata metadata) {
        log.info("Owner registration attempt for userLoginId: {}, institute: {}", request.userLoginId(), request.instituteName());

        if (userDirectoryPort.findByUserLoginId(request.userLoginId()).isPresent()) {
            throw new UserLoginIdAlreadyExistsException("User login ID already registered: " + request.userLoginId());
        }

        TenantCreationResult tenantResult = tenantServicePort.createTenant(
                request.instituteName(),
                request.businessRegistrationNumber(),
                request.userLoginId(),
                request.name()
        );

        if (!tenantResult.success()) {
            throw new TenantCreationFailedException("Failed to create tenant: " + tenantResult.message());
        }

        log.info("Tenant created: slug={}, tenantId={}", tenantResult.tenantSlug(), tenantResult.tenantId());

        String passwordHash = passwordEncoder.encode(request.password());

        // Create the OWNER user inside the new tenant schema (full identity).
        UserData user = withinTenantContext(tenantResult.tenantSlug(), tenantResult.tenantId(), () ->
                userLookupPort.createUser(
                        request.userLoginId(),
                        request.name(),
                        request.phone(),
                        passwordHash,
                        Role.OWNER
                )
        );

        // Mirror a slim row into the public.users directory so root-entry login
        // (lumie-edu.com) can resolve this owner's tenant. Email is null for now;
        // future password-recovery flows will populate it.
        userDirectoryPort.createDirectoryEntry(
                user.id(),
                request.userLoginId(),
                tenantResult.tenantId(),
                null
        );

        adminServicePort.createOwnerAdmin(
                tenantResult.tenantSlug(),
                user.id(),
                request.userLoginId(),
                request.name(),
                request.phone(),
                null
        );

        IssuedTokens issued = issueTokensForLogin(user, tenantResult.tenantSlug(), tenantResult.tenantId(), metadata);

        log.info("Owner registration successful: userId={}, tenantSlug={}", user.id(), tenantResult.tenantSlug());

        return LoginResponse.of(issued.tokenPair(), user.toUserResponse(tenantResult.tenantSlug()));
    }

    @Override
    public void logout(String token) {
        log.info("Logout request received");

        TokenClaims claims = jwtTokenProvider.parseToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid token"));

        long ttl = claims.exp().getEpochSecond() - Instant.now().getEpochSecond();
        if (ttl > 0) {
            tokenPersistencePort.blacklistToken(claims.jti(), ttl);
        }

        deleteSessionAndRefreshToken(claims.tenantSlug(), claims.getUserId(), claims.sid());

        log.info("Logout successful for user: {} on tenant: {}", claims.sub(), claims.tenantSlug());
    }

    @Override
    public void logoutAll(String token) {
        log.info("Logout all sessions request received");

        TokenClaims claims = jwtTokenProvider.parseToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid token"));

        long ttl = claims.exp().getEpochSecond() - Instant.now().getEpochSecond();
        if (ttl > 0) {
            tokenPersistencePort.blacklistToken(claims.jti(), ttl);
        }

        tokenPersistencePort.deleteAllRefreshTokensForUser(claims.getUserId());
        tokenPersistencePort.deleteAllSessionsForUser(claims.tenantSlug(), claims.getUserId());

        log.info("Logout all sessions successful for user: {}", claims.sub());
    }

    @Override
    public TokenResponse refresh(RefreshTokenRequest request) {
        log.info("Token refresh request received");

        TokenClaims claims = jwtTokenProvider.parseToken(request.refreshToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (tokenPersistencePort.isBlacklisted(claims.jti())) {
            throw new InvalidTokenException("Token has been revoked");
        }

        tokenPersistencePort.findRefreshToken(claims.getUserId(), claims.jti())
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        // Refresh tokens carry tenantSlug/tenantId in their claims, so we can
        // restore tenant context directly without re-querying the directory.
        TenantData tenant = tenantServicePort.validateTenant(claims.tenantSlug())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", claims.tenantSlug()));

        if (!tenant.isActive()) {
            throw new IllegalStateException("Tenant is not active");
        }

        UserData user = withinTenantContext(tenant.slug(), tenant.id(),
                () -> userLookupPort.findById(claims.getUserId())
                        .orElseThrow(() -> new ResourceNotFoundException("User", claims.sub())));

        if (!user.enabled()) {
            throw new IllegalStateException("User account is disabled");
        }

        tokenPersistencePort.deleteRefreshToken(claims.getUserId(), claims.jti());

        String sid = claims.sid();
        TokenPair newTokenPair = generateTokenPair(user, tenant.slug(), tenant.id(), sid);
        TokenClaims newRefreshClaims = jwtTokenProvider.parseToken(newTokenPair.refreshToken())
                .orElseThrow(() -> new IllegalStateException("Failed to parse generated refresh token"));
        TokenClaims newAccessClaims = jwtTokenProvider.parseToken(newTokenPair.accessToken())
                .orElseThrow(() -> new IllegalStateException("Failed to parse generated access token"));

        saveRefreshToken(user.id(), tenant.slug(), tenant.id(), user.role(), newRefreshClaims);
        updateSessionTokenJtis(tenant.slug(), sid, newAccessClaims.jti(), newRefreshClaims.jti());

        log.info("Token refresh successful for user: {}", user.id());

        return TokenResponse.from(newTokenPair);
    }

    @Override
    public UserResponse updateProfile(Long userId, String tenantSlug, UpdateProfileRequest request) {
        log.info("Profile update request for user: {}", userId);

        boolean updated = userLookupPort.updateProfile(userId, request.name(), request.phone(), request.email());
        if (!updated) {
            throw new ResourceNotFoundException("User", String.valueOf(userId));
        }

        UserData user = userLookupPort.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", String.valueOf(userId)));

        return user.toUserResponse(tenantSlug);
    }

    @Override
    public void changePassword(Long userId, String tenantSlug, ChangePasswordRequest request) {
        log.info("Password change request for user: {}", userId);

        UserData user = userLookupPort.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", String.valueOf(userId)));

        if (!passwordEncoder.matches(request.currentPassword(), user.passwordHash())) {
            throw new IllegalStateException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.newPassword(), user.passwordHash())) {
            throw new IllegalArgumentException("New password must differ from the current password");
        }

        String newHash = passwordEncoder.encode(request.newPassword());
        userLookupPort.resetPassword(userId, newHash);

        tokenPersistencePort.deleteAllRefreshTokensForUser(userId);
        tokenPersistencePort.deleteAllSessionsForUser(tenantSlug, userId);

        log.info("Password changed and refresh sessions revoked for user: {}", userId);
    }

    @Override
    public void revokeSession(Long userId, String tenantSlug, String sid) {
        log.info("Revoke session request for user: {} sid: {}", userId, sid);

        Optional<String> sessionDataOpt = tokenPersistencePort.findSession(tenantSlug, sid);
        if (sessionDataOpt.isEmpty()) {
            throw new ResourceNotFoundException("Session", sid);
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(sessionDataOpt.get(), Map.class);
            String accessJti = (String) data.get("accessJti");
            String refreshJti = (String) data.get("refreshJti");
            if (accessJti != null) {
                tokenPersistencePort.blacklistToken(accessJti, jwtTokenProvider.getAccessTokenExpiration());
            }
            if (refreshJti != null) {
                tokenPersistencePort.deleteRefreshToken(userId, refreshJti);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse session data for sid: {}", sid, e);
        }

        tokenPersistencePort.deleteSession(tenantSlug, sid);
        tokenPersistencePort.removeSessionFromIndex(tenantSlug, userId, sid);

        log.info("Session revoked for user: {} sid: {}", userId, sid);
    }

    @Override
    public void revokeOtherSessions(Long userId, String tenantSlug, String currentSid) {
        log.info("Revoke other sessions request for user: {}", userId);

        Set<String> allSids = tokenPersistencePort.findAllSessionJtis(tenantSlug, userId);
        for (String sid : allSids) {
            if (!sid.equals(currentSid)) {
                revokeSession(userId, tenantSlug, sid);
            }
        }

        log.info("Other sessions revoked for user: {}", userId);
    }

    private IssuedTokens issueTokensForLogin(UserData user, String tenantSlug, Long tenantId, SessionMetadata metadata) {
        enforceSessionLimitByCategory(tenantSlug, user.id(), metadata.category());

        String sid = UUID.randomUUID().toString();
        TokenPair tokenPair = generateTokenPair(user, tenantSlug, tenantId, sid);

        TokenClaims refreshClaims = jwtTokenProvider.parseToken(tokenPair.refreshToken())
                .orElseThrow(() -> new IllegalStateException("Failed to parse generated refresh token"));
        TokenClaims accessClaims = jwtTokenProvider.parseToken(tokenPair.accessToken())
                .orElseThrow(() -> new IllegalStateException("Failed to parse generated access token"));

        saveRefreshToken(user.id(), tenantSlug, tenantId, user.role(), refreshClaims);
        saveSessionData(tenantSlug, user.id(), sid, accessClaims.jti(), refreshClaims.jti(), metadata);

        return new IssuedTokens(tokenPair, sid, accessClaims.jti(), refreshClaims.jti());
    }

    private void enforceSessionLimitByCategory(String tenantSlug, Long userId, DeviceCategory category) {
        if (category == null || category == DeviceCategory.UNKNOWN) {
            return;
        }
        Set<String> allSids = tokenPersistencePort.findAllSessionJtis(tenantSlug, userId);
        if (allSids.isEmpty()) {
            return;
        }
        Map<String, String> sessionDataMap = tokenPersistencePort.findSessionsBulk(tenantSlug, allSids);

        for (Map.Entry<String, String> entry : sessionDataMap.entrySet()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = objectMapper.readValue(entry.getValue(), Map.class);
                String existingCategory = (String) data.get("deviceCategory");
                if (!category.name().equals(existingCategory)) {
                    continue;
                }
                String sid = entry.getKey();
                String accessJti = (String) data.get("accessJti");
                String refreshJti = (String) data.get("refreshJti");
                if (accessJti != null) {
                    tokenPersistencePort.blacklistToken(accessJti, jwtTokenProvider.getAccessTokenExpiration());
                }
                if (refreshJti != null) {
                    tokenPersistencePort.deleteRefreshToken(userId, refreshJti);
                }
                tokenPersistencePort.deleteSession(tenantSlug, sid);
                tokenPersistencePort.removeSessionFromIndex(tenantSlug, userId, sid);
                log.info("Revoked existing {} session for user {}: sid={}", category, userId, sid);
            } catch (JsonProcessingException e) {
                log.error("Failed to parse session data during category enforcement for sid: {}", entry.getKey(), e);
            }
        }
    }

    private TokenPair generateTokenPair(UserData user, String tenantSlug, Long tenantId, String sid) {
        String accessJti = UUID.randomUUID().toString();
        String refreshJti = UUID.randomUUID().toString();

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.id(), tenantSlug, tenantId, user.role(), accessJti, sid);
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.id(), tenantSlug, tenantId, user.role(), refreshJti, sid);

        return TokenPair.of(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpiration(),
                jwtTokenProvider.getRefreshTokenExpiration()
        );
    }

    private <T> T withinTenantContext(String tenantSlug, Long tenantId, java.util.function.Supplier<T> action) {
        return TenantContextHolder.withinContext(tenantSlug, tenantId, action);
    }

    private void saveRefreshToken(Long userId, String tenantSlug, Long tenantId,
                                   Role role, TokenClaims refreshClaims) {
        AuthToken authToken = AuthToken.create(
                refreshClaims.jti(),
                userId,
                tenantSlug,
                tenantId,
                role,
                refreshClaims.exp()
        );
        tokenPersistencePort.saveRefreshToken(authToken);
    }

    private void saveSessionData(String tenantSlug, Long userId, String sid,
                                  String accessJti, String refreshJti, SessionMetadata metadata) {
        Map<String, Object> sessionData = Map.of(
                "userId", userId,
                "device", metadata.device(),
                "deviceCategory", metadata.category().name(),
                "createdAt", Instant.now().toEpochMilli(),
                "accessJti", accessJti,
                "refreshJti", refreshJti
        );

        try {
            String json = objectMapper.writeValueAsString(sessionData);
            long ttl = jwtTokenProvider.getRefreshTokenExpiration();
            tokenPersistencePort.saveSession(tenantSlug, sid, json, ttl);
            tokenPersistencePort.addSessionToIndex(tenantSlug, userId, sid);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize session data", e);
        }
    }

    private void updateSessionTokenJtis(String tenantSlug, String sid, String newAccessJti, String newRefreshJti) {
        Optional<String> sessionDataOpt = tokenPersistencePort.findSession(tenantSlug, sid);
        if (sessionDataOpt.isEmpty()) {
            log.warn("Session data missing for sid {} during refresh; skipping metadata update", sid);
            return;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = new HashMap<>(objectMapper.readValue(sessionDataOpt.get(), Map.class));
            data.put("accessJti", newAccessJti);
            data.put("refreshJti", newRefreshJti);
            String json = objectMapper.writeValueAsString(data);
            long ttl = jwtTokenProvider.getRefreshTokenExpiration();
            tokenPersistencePort.saveSession(tenantSlug, sid, json, ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to update session metadata for sid: " + sid, e);
        }
    }

    private void deleteSessionAndRefreshToken(String tenantSlug, Long userId, String sid) {
        if (sid == null) {
            return;
        }
        Optional<String> sessionDataOpt = tokenPersistencePort.findSession(tenantSlug, sid);
        if (sessionDataOpt.isPresent()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = objectMapper.readValue(sessionDataOpt.get(), Map.class);
                String refreshJti = (String) data.get("refreshJti");
                if (refreshJti != null) {
                    tokenPersistencePort.deleteRefreshToken(userId, refreshJti);
                }
            } catch (JsonProcessingException e) {
                log.error("Failed to parse session data during logout for sid: {}", sid, e);
            }
        }
        tokenPersistencePort.deleteSession(tenantSlug, sid);
        tokenPersistencePort.removeSessionFromIndex(tenantSlug, userId, sid);
    }

    private record IssuedTokens(TokenPair tokenPair, String sid, String accessJti, String refreshJti) {
    }

    public static class InvalidCredentialsException extends com.lumie.common.exception.BusinessException {
        public InvalidCredentialsException(String message) {
            super(com.lumie.common.exception.CommonErrorCode.UNAUTHORIZED, message);
        }
    }

    public static class InvalidTokenException extends com.lumie.common.exception.BusinessException {
        public InvalidTokenException(String message) {
            super(com.lumie.common.exception.CommonErrorCode.UNAUTHORIZED, message);
        }
    }

    public static class UserLoginIdAlreadyExistsException extends RuntimeException {
        public UserLoginIdAlreadyExistsException(String message) {
            super(message);
        }
    }

    public static class TenantCreationFailedException extends RuntimeException {
        public TenantCreationFailedException(String message) {
            super(message);
        }
    }
}
