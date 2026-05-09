package com.lumie.auth.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.auth.application.port.out.TokenPersistencePort;
import com.lumie.auth.application.port.out.TokenProviderPort;
import com.lumie.auth.application.port.out.UserLookupPort.UserData;
import com.lumie.auth.domain.entity.AuthToken;
import com.lumie.auth.domain.vo.DeviceCategory;
import com.lumie.auth.domain.vo.SessionMetadata;
import com.lumie.auth.domain.vo.TokenClaims;
import com.lumie.auth.domain.vo.TokenPair;
import com.lumie.common.auth.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Shared helpers for login/refresh/register/registerOwner flows: token
 * issuance, session metadata persistence, single-device-category enforcement.
 *
 * <p>Was previously private methods on a monolithic {@code AuthCommandService}.
 * Extracted so the use-case-specific services (Session / Registration /
 * Profile) can stay narrowly focused on their own concern.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginSessionHelper {

    private final TokenPersistencePort tokenPersistencePort;
    private final TokenProviderPort tokenProvider;
    private final ObjectMapper objectMapper;

    public IssuedTokens issueTokensForLogin(UserData user, String tenantSlug, Long tenantId, SessionMetadata metadata) {
        enforceSessionLimitByCategory(tenantSlug, user.id(), metadata.category());

        String sid = UUID.randomUUID().toString();
        TokenPair tokenPair = generateTokenPair(user, tenantSlug, tenantId, sid);

        TokenClaims refreshClaims = tokenProvider.parseToken(tokenPair.refreshToken())
                .orElseThrow(() -> new IllegalStateException("Failed to parse generated refresh token"));
        TokenClaims accessClaims = tokenProvider.parseToken(tokenPair.accessToken())
                .orElseThrow(() -> new IllegalStateException("Failed to parse generated access token"));

        saveRefreshToken(user.id(), tenantSlug, tenantId, user.role(), refreshClaims);
        saveSessionData(tenantSlug, user.id(), sid, accessClaims.jti(), refreshClaims.jti(), metadata);

        return new IssuedTokens(tokenPair, sid, accessClaims.jti(), refreshClaims.jti());
    }

    public TokenPair generateTokenPair(UserData user, String tenantSlug, Long tenantId, String sid) {
        String accessJti = UUID.randomUUID().toString();
        String refreshJti = UUID.randomUUID().toString();

        String accessToken = tokenProvider.generateAccessToken(
                user.id(), tenantSlug, tenantId, user.role(), accessJti, sid);
        String refreshToken = tokenProvider.generateRefreshToken(
                user.id(), tenantSlug, tenantId, user.role(), refreshJti, sid);

        return TokenPair.of(
                accessToken,
                refreshToken,
                tokenProvider.getAccessTokenExpiration(),
                tokenProvider.getRefreshTokenExpiration()
        );
    }

    public void saveRefreshToken(Long userId, String tenantSlug, Long tenantId,
                                 Role role, TokenClaims refreshClaims) {
        AuthToken authToken = AuthToken.create(
                refreshClaims.jti(), userId, tenantSlug, tenantId, role, refreshClaims.exp());
        tokenPersistencePort.saveRefreshToken(authToken);
    }

    public void updateSessionTokenJtis(String tenantSlug, String sid, String newAccessJti, String newRefreshJti) {
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
            long ttl = tokenProvider.getRefreshTokenExpiration();
            tokenPersistencePort.saveSession(tenantSlug, sid, json, ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to update session metadata for sid: " + sid, e);
        }
    }

    public void deleteSessionAndRefreshToken(String tenantSlug, Long userId, String sid) {
        if (sid == null) return;
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

    public long getAccessTokenExpiration() {
        return tokenProvider.getAccessTokenExpiration();
    }

    private void enforceSessionLimitByCategory(String tenantSlug, Long userId, DeviceCategory category) {
        if (category == null || category == DeviceCategory.UNKNOWN) return;
        Set<String> allSids = tokenPersistencePort.findAllSessionJtis(tenantSlug, userId);
        if (allSids.isEmpty()) return;
        Map<String, String> sessionDataMap = tokenPersistencePort.findSessionsBulk(tenantSlug, allSids);

        for (Map.Entry<String, String> entry : sessionDataMap.entrySet()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = objectMapper.readValue(entry.getValue(), Map.class);
                String existingCategory = (String) data.get("deviceCategory");
                if (!category.name().equals(existingCategory)) continue;
                String sid = entry.getKey();
                String accessJti = (String) data.get("accessJti");
                String refreshJti = (String) data.get("refreshJti");
                if (accessJti != null) {
                    tokenPersistencePort.blacklistToken(accessJti, tokenProvider.getAccessTokenExpiration());
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
            long ttl = tokenProvider.getRefreshTokenExpiration();
            tokenPersistencePort.saveSession(tenantSlug, sid, json, ttl);
            tokenPersistencePort.addSessionToIndex(tenantSlug, userId, sid);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize session data", e);
        }
    }

    public record IssuedTokens(TokenPair tokenPair, String sid, String accessJti, String refreshJti) {
    }
}
