package com.lumie.auth.application.service;

import com.lumie.auth.application.dto.response.LoginResponse;
import com.lumie.auth.application.port.in.OAuth2LoginUseCase;
import com.lumie.auth.application.port.out.AuthEventPublisherPort;
import com.lumie.auth.application.port.out.TenantServicePort;
import com.lumie.auth.application.port.out.TenantServicePort.TenantData;
import com.lumie.auth.application.port.out.TokenPersistencePort;
import com.lumie.auth.application.port.out.UserLookupPort;
import com.lumie.auth.application.port.out.UserLookupPort.UserData;
import com.lumie.auth.domain.entity.AuthToken;
import com.lumie.auth.domain.vo.TokenClaims;
import com.lumie.auth.domain.vo.TokenPair;
import com.lumie.auth.infrastructure.config.OAuth2Config;
import com.lumie.auth.infrastructure.security.JwtTokenProvider;
import com.lumie.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2AuthService implements OAuth2LoginUseCase {

    private final OAuth2Config oAuth2Config;
    private final TenantServicePort tenantServicePort;
    private final UserLookupPort userLookupPort;
    private final TokenPersistencePort tokenPersistencePort;
    private final AuthEventPublisherPort eventPublisherPort;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public String getAuthorizationUrl(String provider, String tenantSlug) {
        log.info("Generating OAuth2 authorization URL for provider: {} tenant: {}", provider, tenantSlug);

        // Validate tenant exists
        tenantServicePort.validateTenant(tenantSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantSlug));

        // Generate state with tenant info: base64({tenantSlug}:{nonce})
        String nonce = generateNonce();
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

    @Override
    public LoginResponse handleCallback(String provider, String code, String state) {
        log.info("Handling OAuth2 callback for provider: {}", provider);

        // 1. Decode state to get tenant slug
        String[] stateParts = decodeState(state);
        String tenantSlug = stateParts[0];

        // 2. Validate tenant
        TenantData tenant = tenantServicePort.validateTenant(tenantSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantSlug));

        if (!tenant.isActive()) {
            throw new IllegalStateException("Tenant is not active: " + tenantSlug);
        }

        OAuth2Config.ProviderConfig config = getProviderConfig(provider);

        // 3. Exchange code for access token
        Map<String, String> tokenResponse = exchangeCodeForToken(provider, code, config);
        String accessToken = tokenResponse.get("access_token");

        // 4. Get user info from provider
        OAuth2UserInfo userInfo = getUserInfo(provider, accessToken, config);

        // 5. Find or create user in tenant schema
        UserData user = userLookupPort.findOrCreateOAuth2User(
                tenant.schemaName(),
                userInfo.email(),
                userInfo.name(),
                provider
        );

        // 6. Generate tokens
        TokenPair tokenPair = generateTokenPair(user, tenantSlug, tenant.id());

        // 7. Save refresh token
        saveRefreshToken(user.id(), tenantSlug, tenant.id(), user.role(), tokenPair);

        // 8. Publish login event
        eventPublisherPort.publishLoginEvent(user.id(), tenantSlug, user.email(), provider);

        log.info("OAuth2 login successful for user: {} via {}", user.email(), provider);

        return LoginResponse.of(tokenPair, user.toUserResponse(tenantSlug, tenant.id()));
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
                throw new IllegalArgumentException("Invalid state format");
            }
            return parts;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to decode state parameter", e);
        }
    }

    private OAuth2Config.ProviderConfig getProviderConfig(String provider) {
        return switch (provider.toLowerCase()) {
            case "google" -> oAuth2Config.getGoogle();
            case "kakao" -> oAuth2Config.getKakao();
            default -> throw new IllegalArgumentException("Unsupported OAuth2 provider: " + provider);
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> exchangeCodeForToken(String provider, String code,
                                                      OAuth2Config.ProviderConfig config) {
        Map<String, String> params = Map.of(
                "grant_type", "authorization_code",
                "client_id", config.getClientId(),
                "client_secret", config.getClientSecret(),
                "redirect_uri", config.getRedirectUri(),
                "code", code
        );

        return restTemplate.postForObject(config.getTokenUri(), params, Map.class);
    }

    @SuppressWarnings("unchecked")
    private OAuth2UserInfo getUserInfo(String provider, String accessToken,
                                        OAuth2Config.ProviderConfig config) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setBearerAuth(accessToken);

        org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);

        Map<String, Object> response = restTemplate.exchange(
                config.getUserInfoUri(),
                org.springframework.http.HttpMethod.GET,
                entity,
                Map.class
        ).getBody();

        return switch (provider.toLowerCase()) {
            case "google" -> new OAuth2UserInfo(
                    (String) response.get("email"),
                    (String) response.get("name")
            );
            case "kakao" -> {
                Map<String, Object> kakaoAccount = (Map<String, Object>) response.get("kakao_account");
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                yield new OAuth2UserInfo(
                        (String) kakaoAccount.get("email"),
                        (String) profile.get("nickname")
                );
            }
            default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
        };
    }

    private TokenPair generateTokenPair(UserData user, String tenantSlug, Long tenantId) {
        String accessJti = UUID.randomUUID().toString();
        String refreshJti = UUID.randomUUID().toString();

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.id(), tenantSlug, tenantId, user.role(), accessJti);
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.id(), tenantSlug, tenantId, user.role(), refreshJti);

        return TokenPair.of(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpiration(),
                jwtTokenProvider.getRefreshTokenExpiration()
        );
    }

    private void saveRefreshToken(Long userId, String tenantSlug, Long tenantId,
                                   com.lumie.auth.domain.vo.Role role, TokenPair tokenPair) {
        TokenClaims refreshClaims = jwtTokenProvider.parseToken(tokenPair.refreshToken())
                .orElseThrow(() -> new IllegalStateException("Failed to parse generated refresh token"));

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

    private record OAuth2UserInfo(String email, String name) {
    }
}
