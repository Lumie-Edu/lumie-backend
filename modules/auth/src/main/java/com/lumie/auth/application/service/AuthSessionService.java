package com.lumie.auth.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.auth.application.dto.request.LoginRequest;
import com.lumie.auth.application.dto.request.RefreshTokenRequest;
import com.lumie.auth.application.dto.response.LoginResponse;
import com.lumie.auth.application.dto.response.TokenResponse;
import com.lumie.auth.application.exception.AuthErrors.InvalidCredentialsException;
import com.lumie.auth.application.exception.AuthErrors.InvalidTokenException;
import com.lumie.auth.application.exception.AuthErrors.SessionReplacedByLoginException;
import com.lumie.auth.application.exception.AuthErrors.TenantInactiveException;
import com.lumie.auth.application.exception.AuthErrors.TokenIssuanceFailedException;
import com.lumie.auth.application.exception.AuthErrors.UserDisabledException;
import com.lumie.auth.application.port.out.TokenPersistencePort;
import com.lumie.auth.application.port.out.TokenProviderPort;
import com.lumie.tenant.api.TenantService;
import com.lumie.tenant.api.TenantService.TenantData;
import com.lumie.auth.application.port.out.UserDirectoryPort;
import com.lumie.auth.application.port.out.UserLookupPort;
import com.lumie.auth.application.port.out.UserLookupPort.UserData;
import com.lumie.auth.application.service.LoginSessionHelper.IssuedTokens;
import com.lumie.auth.domain.vo.SessionMetadata;
import com.lumie.auth.domain.vo.SessionRevocationReason;
import com.lumie.auth.domain.vo.TokenClaims;
import com.lumie.auth.domain.vo.TokenPair;
import com.lumie.common.exception.ResourceNotFoundException;
import com.lumie.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Session-lifecycle use cases: login, logout, refresh, revoke.
 *
 * <p>Login routes via {@link TenantContextHolder} populated from
 * {@code X-Tenant-Slug} (portal entry), via {@code customId} on tenant public
 * pages, or via the OWNER directory (root entry). All paths converge on a
 * tenant-scoped {@code users} lookup.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthSessionService {

    private final TenantService tenantService;
    private final UserLookupPort userLookupPort;
    private final UserDirectoryPort userDirectoryPort;
    private final TokenPersistencePort tokenPersistencePort;
    private final TokenProviderPort tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final LoginSessionHelper loginSessionHelper;

    public LoginResponse login(LoginRequest request, SessionMetadata metadata) {
        log.info("Login attempt for userLoginId: {}", request.userLoginId());

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

            IssuedTokens issued = loginSessionHelper.issueTokensForLogin(user, tenant.slug(), tenant.id(), metadata);
            log.info("Login successful for user: {} on tenant: {}", user.id(), tenant.slug());
            return LoginResponse.of(issued.tokenPair(), user.toUserResponse(
                    tenant.slug(),
                    tenant.customId(),
                    tenant.customDomain(),
                    tenant.onboardingCompletedAt()
            ));
        });
    }

    private TenantData resolveLoginTenant(LoginRequest request) {
        String headerSlug = TenantContextHolder.getTenant();
        if (headerSlug != null && !headerSlug.isBlank()) {
            // Portal entry — header provides the tenant.
            TenantData tenant = tenantService.getTenantBySlug(headerSlug)
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid user login ID or password"));
            if (!tenant.isActive()) {
                throw new TenantInactiveException("Tenant is not active");
            }
            return tenant;
        }
        if (request.customId() != null && !request.customId().isBlank()) {
            // Tenant public page entry — URL customId provides the tenant when
            // no X-Tenant-Slug header is available. The lookup is active-only,
            // so missing and inactive customIds both use the generic login
            // error without falling back to the OWNER directory.
            return tenantService.getTenantByCustomId(request.customId())
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid user login ID or password"));
        }
        // Root entry — directory lookup. Must be an OWNER because only OWNERs
        // are mirrored into public.owner_directory; otherwise reject with the
        // standard generic error.
        UserDirectoryPort.DirectoryEntry entry = userDirectoryPort.findByUserLoginId(request.userLoginId())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid user login ID or password"));

        TenantData tenant = tenantService.getTenant(entry.tenantId())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid user login ID or password"));
        if (!tenant.isActive()) {
            throw new TenantInactiveException("Tenant is not active");
        }
        return tenant;
    }

    public void logout(String token) {
        TokenClaims claims = tokenProvider.parseToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid token"));
        long ttl = claims.exp().getEpochSecond() - Instant.now().getEpochSecond();
        if (ttl > 0) {
            tokenPersistencePort.blacklistToken(claims.jti(), ttl);
        }
        loginSessionHelper.deleteSessionAndRefreshToken(claims.tenantSlug(), claims.getUserId(), claims.sid());
        log.info("Logout successful for user: {} on tenant: {}", claims.sub(), claims.tenantSlug());
    }

    public void logoutAll(String token) {
        TokenClaims claims = tokenProvider.parseToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid token"));
        long ttl = claims.exp().getEpochSecond() - Instant.now().getEpochSecond();
        if (ttl > 0) {
            tokenPersistencePort.blacklistToken(claims.jti(), ttl);
        }
        tokenPersistencePort.deleteAllRefreshTokensForUser(claims.getUserId());
        tokenPersistencePort.deleteAllSessionsForUser(claims.tenantSlug(), claims.getUserId());
        log.info("Logout all sessions successful for user: {}", claims.sub());
    }

    public TokenResponse refresh(RefreshTokenRequest request) {
        log.info("Token refresh request received");

        TokenClaims claims = tokenProvider.parseToken(request.refreshToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (tokenPersistencePort.isBlacklisted(claims.jti())) {
            throw new InvalidTokenException("Token has been revoked");
        }
        if (tokenPersistencePort.findRefreshToken(claims.getUserId(), claims.jti()).isEmpty()) {
            Optional<SessionRevocationReason> revocationReason =
                    tokenPersistencePort.findSessionRevocationReason(claims.tenantSlug(), claims.sid());
            if (revocationReason.filter(SessionRevocationReason.SESSION_REPLACED_BY_LOGIN::equals).isPresent()) {
                throw new SessionReplacedByLoginException();
            }
            throw new InvalidTokenException("Refresh token not found");
        }

        TenantData tenant = tenantService.getTenantBySlug(claims.tenantSlug())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", claims.tenantSlug()));
        if (!tenant.isActive()) {
            throw new TenantInactiveException("Tenant is not active");
        }

        UserData user = TenantContextHolder.withinContext(tenant.slug(), tenant.id(),
                () -> userLookupPort.findById(claims.getUserId())
                        .orElseThrow(() -> new ResourceNotFoundException("User", claims.sub())));
        if (!user.enabled()) {
            throw new UserDisabledException("User account is disabled");
        }

        tokenPersistencePort.deleteRefreshToken(claims.getUserId(), claims.jti());

        String sid = claims.sid();
        TokenPair newTokenPair = loginSessionHelper.generateTokenPair(user, tenant.slug(), tenant.id(), sid);
        TokenClaims newRefreshClaims = tokenProvider.parseToken(newTokenPair.refreshToken())
                .orElseThrow(() -> new TokenIssuanceFailedException("Failed to parse generated refresh token"));
        TokenClaims newAccessClaims = tokenProvider.parseToken(newTokenPair.accessToken())
                .orElseThrow(() -> new TokenIssuanceFailedException("Failed to parse generated access token"));

        loginSessionHelper.saveRefreshToken(user.id(), tenant.slug(), tenant.id(), user.role(), newRefreshClaims);
        loginSessionHelper.updateSessionTokenJtis(tenant.slug(), sid, newAccessClaims.jti(), newRefreshClaims.jti());

        log.info("Token refresh successful for user: {}", user.id());
        return TokenResponse.from(newTokenPair);
    }

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
                tokenPersistencePort.blacklistToken(accessJti, loginSessionHelper.getAccessTokenExpiration());
            }
            if (refreshJti != null) {
                tokenPersistencePort.deleteRefreshToken(userId, refreshJti);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse session data for sid: {}", sid, e);
        }

        tokenPersistencePort.deleteSession(tenantSlug, sid);
        tokenPersistencePort.removeSessionFromIndex(tenantSlug, userId, sid);
    }

    public void revokeOtherSessions(Long userId, String tenantSlug, String currentSid) {
        Set<String> allSids = tokenPersistencePort.findAllSessionJtis(tenantSlug, userId);
        for (String sid : allSids) {
            if (!sid.equals(currentSid)) {
                revokeSession(userId, tenantSlug, sid);
            }
        }
    }
}
