package com.lumie.auth.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.auth.application.dto.response.LoginResponse;
import com.lumie.auth.application.exception.AuthErrors.InvalidStateException;
import com.lumie.auth.application.exception.AuthErrors.SessionSerializationFailedException;
import com.lumie.auth.application.exception.AuthErrors.TenantInactiveException;
import com.lumie.auth.application.exception.AuthErrors.TokenIssuanceFailedException;
import com.lumie.auth.application.exception.AuthErrors.UnsupportedOAuth2ProviderException;
import com.lumie.auth.application.port.out.TokenPersistencePort;
import com.lumie.tenant.api.TenantService;
import com.lumie.tenant.api.TenantService.TenantData;
import com.lumie.auth.application.port.out.UserLookupPort;
import com.lumie.auth.application.port.out.UserLookupPort.UserData;
import com.lumie.auth.domain.entity.AuthToken;
import com.lumie.auth.domain.vo.DeviceCategory;
import com.lumie.auth.domain.vo.SessionMetadata;
import com.lumie.auth.domain.vo.TokenClaims;
import com.lumie.auth.domain.vo.TokenPair;
import com.lumie.auth.adapter.out.config.OAuth2Config;
import com.lumie.auth.application.port.out.TokenProviderPort;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class OAuth2AuthService {

    private final OAuth2Config oAuth2Config;
    private final TenantService tenantService;
    private final UserLookupPort userLookupPort;
    private final TokenPersistencePort tokenPersistencePort;
    private final TokenProviderPort tokenProvider;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OAuth2AuthService(OAuth2Config oAuth2Config,
                              TenantService tenantService,
                              UserLookupPort userLookupPort,
                              TokenPersistencePort tokenPersistencePort,
                              TokenProviderPort tokenProvider,
                              RestClient.Builder restClientBuilder,
                              ObjectMapper objectMapper) {
        this.oAuth2Config = oAuth2Config;
        this.tenantService = tenantService;
        this.userLookupPort = userLookupPort;
        this.tokenPersistencePort = tokenPersistencePort;
        this.tokenProvider = tokenProvider;
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long OAUTH2_NONCE_TTL_SECONDS = 600;

    public String getAuthorizationUrl(String provider, String tenantSlug) {
        log.info("Generating OAuth2 authorization URL for provider: {} tenant: {}", provider, tenantSlug);

        tenantService.getTenantBySlug(tenantSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantSlug));

        String nonce = generateNonce();
        tokenPersistencePort.saveOAuth2Nonce(nonce, tenantSlug, OAUTH2_NONCE_TTL_SECONDS);
        String state = encodeState(tenantSlug, nonce);

        OAuth2Config.ProviderConfig config = getProviderConfig(provider);

        String authUrl = String.format(
                "%s?client_id=%s&redirect_uri=%s&response_type=code&scope=%s&state=%s",
                config.getAuthorizationUri(),
                URLEncoder.encode(config.getClientId(), StandardCharsets.UTF_8),
                URLEncoder.encode(config.getRedirectUri(), StandardCharsets.UTF_8),
                URLEncoder.encode(config.getScope(), StandardCharsets.UTF_8),
                URLEncoder.encode(state, StandardCharsets.UTF_8)
        );

        log.debug("Generated authorization URL for provider: {}", provider);
        return authUrl;
    }

    public LoginResponse handleCallback(String provider, String code, String state, SessionMetadata metadata) {
        log.info("Handling OAuth2 callback for provider: {}", provider);

        String[] stateParts = decodeState(state);
        String tenantSlug = stateParts[0];
        String nonce = stateParts[1];

        String storedTenantSlug = tokenPersistencePort.consumeOAuth2Nonce(nonce)
                .orElseThrow(() -> new InvalidStateException("Invalid or expired OAuth2 state"));
        if (!storedTenantSlug.equals(tenantSlug)) {
            throw new InvalidStateException("OAuth2 state tenant mismatch");
        }

        TenantData tenant = tenantService.getTenantBySlug(tenantSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantSlug));

        if (!tenant.isActive()) {
            throw new TenantInactiveException("Tenant is not active: " + tenantSlug);
        }

        OAuth2Config.ProviderConfig config = getProviderConfig(provider);

        Map<String, String> tokenResponse = exchangeCodeForToken(provider, code, config);
        String accessToken = tokenResponse.get("access_token");

        OAuth2UserInfo userInfo = getUserInfo(provider, accessToken, config);

        String userLoginId = provider.toLowerCase() + "_" + userInfo.providerId();

        // Set tenant context for the user lookup/create — Hibernate routes
        // findOrCreateOAuth2User to tenant_*.users via the resolver.
        UserData user = com.lumie.common.tenant.TenantContextHolder.withinContext(
                tenantSlug, tenant.id(),
                () -> userLookupPort.findOrCreateOAuth2User(userLoginId, userInfo.name(), provider)
        );

        enforceSessionLimitByCategory(tenantSlug, user.id(), metadata.category());

        String sid = UUID.randomUUID().toString();
        TokenPair tokenPair = generateTokenPair(user, tenantSlug, tenant.id(), sid);

        TokenClaims refreshClaims = tokenProvider.parseToken(tokenPair.refreshToken())
                .orElseThrow(() -> new TokenIssuanceFailedException("Failed to parse generated refresh token"));
        TokenClaims accessClaims = tokenProvider.parseToken(tokenPair.accessToken())
                .orElseThrow(() -> new TokenIssuanceFailedException("Failed to parse generated access token"));

        saveRefreshToken(user.id(), tenantSlug, tenant.id(), user.role(), refreshClaims);
        saveSessionData(tenantSlug, user.id(), sid, accessClaims.jti(), refreshClaims.jti(), metadata);

        log.info("OAuth2 login successful for user: {} via {}", user.userLoginId(), provider);

        return LoginResponse.of(tokenPair, user.toUserResponse(tenantSlug));
    }

    private String generateNonce() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String encodeState(String tenantSlug, String nonce) {
        String raw = tenantSlug + ":" + nonce;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String[] decodeState(String state) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":", 2);
            if (parts.length != 2) {
                throw new InvalidStateException("Invalid state format");
            }
            return parts;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidStateException("Failed to decode state parameter", e);
        }
    }

    private OAuth2Config.ProviderConfig getProviderConfig(String provider) {
        return switch (provider.toLowerCase()) {
            case "google" -> oAuth2Config.getGoogle();
            case "kakao" -> oAuth2Config.getKakao();
            case "naver" -> oAuth2Config.getNaver();
            default -> throw new UnsupportedOAuth2ProviderException("Unsupported OAuth2 provider: " + provider);
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> exchangeCodeForToken(String provider, String code,
                                                      OAuth2Config.ProviderConfig config) {
        // RFC 6749 §4.1.3 mandates application/x-www-form-urlencoded for the
        // token endpoint. Google + Naver enforce this strictly; Kakao is
        // lenient but documents form-encoding as the supported shape.
        org.springframework.util.MultiValueMap<String, String> params =
                new org.springframework.util.LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", config.getClientId());
        params.add("client_secret", config.getClientSecret());
        params.add("redirect_uri", config.getRedirectUri());
        params.add("code", code);

        return restClient.post()
                .uri(config.getTokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .body(Map.class);
    }

    @SuppressWarnings("unchecked")
    private OAuth2UserInfo getUserInfo(String provider, String accessToken,
                                        OAuth2Config.ProviderConfig config) {
        Map<String, Object> response = restClient.get()
                .uri(config.getUserInfoUri())
                .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(Map.class);

        return switch (provider.toLowerCase()) {
            case "google" -> new OAuth2UserInfo(
                    (String) response.get("sub"),
                    (String) response.get("name")
            );
            case "kakao" -> {
                Map<String, Object> kakaoAccount = (Map<String, Object>) response.get("kakao_account");
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                yield new OAuth2UserInfo(
                        String.valueOf(response.get("id")),
                        (String) profile.get("nickname")
                );
            }
            case "naver" -> {
                Map<String, Object> naverProfile = (Map<String, Object>) response.get("response");
                yield new OAuth2UserInfo(
                        (String) naverProfile.get("id"),
                        (String) naverProfile.getOrDefault("name",
                                naverProfile.getOrDefault("nickname", "Unknown"))
                );
            }
            default -> throw new UnsupportedOAuth2ProviderException("Unsupported provider: " + provider);
        };
    }

    private TokenPair generateTokenPair(UserData user, String tenantSlug, Long tenantId, String sid) {
        String accessJti = UUID.randomUUID().toString();
        String refreshJti = UUID.randomUUID().toString();

        String accessToken = tokenProvider.generateAccessToken(
                user.id(), user.name(), tenantSlug, tenantId, user.role(), accessJti, sid);
        String refreshToken = tokenProvider.generateRefreshToken(
                user.id(), user.name(), tenantSlug, tenantId, user.role(), refreshJti, sid);

        return TokenPair.of(
                accessToken,
                refreshToken,
                tokenProvider.getAccessTokenExpiration(),
                tokenProvider.getRefreshTokenExpiration()
        );
    }

    private void saveRefreshToken(Long userId, String tenantSlug, Long tenantId,
                                   com.lumie.common.auth.Role role, TokenClaims refreshClaims) {
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
            long ttl = tokenProvider.getRefreshTokenExpiration();
            tokenPersistencePort.saveSession(tenantSlug, sid, json, ttl);
            tokenPersistencePort.addSessionToIndex(tenantSlug, userId, sid);
        } catch (JsonProcessingException e) {
            throw new SessionSerializationFailedException(e);
        }
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

    private record OAuth2UserInfo(String providerId, String name) {
    }
}
