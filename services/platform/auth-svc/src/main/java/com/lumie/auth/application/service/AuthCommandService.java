package com.lumie.auth.application.service;

import com.lumie.auth.application.dto.request.LoginRequest;
import com.lumie.auth.application.dto.request.OwnerRegisterRequest;
import com.lumie.auth.application.dto.request.RefreshTokenRequest;
import com.lumie.auth.application.dto.request.RegisterRequest;
import com.lumie.auth.application.dto.response.LoginResponse;
import com.lumie.auth.application.dto.response.TokenResponse;
import com.lumie.auth.application.port.in.LoginUseCase;
import com.lumie.auth.application.port.in.LogoutUseCase;
import com.lumie.auth.application.port.in.OwnerRegisterUseCase;
import com.lumie.auth.application.port.in.RefreshTokenUseCase;
import com.lumie.auth.application.port.in.RegisterUseCase;
import com.lumie.auth.domain.vo.Role;
import com.lumie.auth.application.port.out.AuthEventPublisherPort;
import com.lumie.auth.application.port.out.TenantServicePort;
import com.lumie.auth.application.port.out.TenantServicePort.TenantCreationResult;
import com.lumie.auth.application.port.out.TenantServicePort.TenantData;
import com.lumie.auth.application.port.out.TokenPersistencePort;
import com.lumie.auth.application.port.out.UserLookupPort;
import com.lumie.auth.application.port.out.UserLookupPort.UserData;
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
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthCommandService implements LoginUseCase, LogoutUseCase, RefreshTokenUseCase, RegisterUseCase, OwnerRegisterUseCase {

    private final TenantServicePort tenantServicePort;
    private final UserLookupPort userLookupPort;
    private final TokenPersistencePort tokenPersistencePort;
    private final AuthEventPublisherPort eventPublisherPort;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginResponse register(RegisterRequest request) {
        log.info("Registration attempt for userLoginId: {} on tenant: {}", request.userLoginId(), request.tenantSlug());

        // 1. Validate tenant via gRPC
        TenantData tenant = tenantServicePort.validateTenant(request.tenantSlug())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", request.tenantSlug()));

        if (!tenant.isActive()) {
            throw new IllegalStateException("Tenant is not active: " + request.tenantSlug());
        }

        // 2. Check if userLoginId already exists (globally unique)
        if (userLookupPort.existsByUserLoginId(request.userLoginId())) {
            throw new UserLoginIdAlreadyExistsException("User login ID already registered: " + request.userLoginId());
        }

        // 3. Hash password and create user in public.users
        String passwordHash = passwordEncoder.encode(request.password());
        UserData user = userLookupPort.createUser(
                request.userLoginId(),
                request.name(),
                request.phone(),
                passwordHash,
                Role.STUDENT,
                tenant.id()
        );

        // 4. Generate tokens
        TokenPair tokenPair = generateTokenPair(user, request.tenantSlug());

        // 5. Save refresh token to Redis
        saveRefreshToken(user.id(), request.tenantSlug(), tenant.id(), user.role(), tokenPair);

        log.info("Registration successful for user: {} on tenant: {}", user.id(), request.tenantSlug());

        return LoginResponse.of(tokenPair, user.toUserResponse(request.tenantSlug()));
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for userLoginId: {}", request.userLoginId());

        // 1. Look up user in public.users by userLoginId
        UserData user = userLookupPort.findByUserLoginId(request.userLoginId())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid user login ID or password"));

        if (!user.enabled()) {
            throw new IllegalStateException("User account is disabled");
        }

        // 2. Verify password
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new InvalidCredentialsException("Invalid user login ID or password");
        }

        // 3. Get tenant info for the token (tenant is determined from user record)
        TenantData tenant = tenantServicePort.validateTenantById(user.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", user.tenantId().toString()));

        if (!tenant.isActive()) {
            throw new IllegalStateException("Tenant is not active");
        }

        // 4. Generate tokens
        TokenPair tokenPair = generateTokenPair(user, tenant.slug());

        // 5. Save refresh token to Redis
        saveRefreshToken(user.id(), tenant.slug(), tenant.id(), user.role(), tokenPair);

        // 6. Publish login event
        eventPublisherPort.publishLoginEvent(user.id(), tenant.slug(), user.userLoginId(), "local");

        log.info("Login successful for user: {} on tenant: {}", user.id(), tenant.slug());

        return LoginResponse.of(tokenPair, user.toUserResponse(tenant.slug()));
    }

    @Override
    public LoginResponse registerOwner(OwnerRegisterRequest request) {
        log.info("Owner registration attempt for userLoginId: {}, institute: {}", request.userLoginId(), request.instituteName());

        // 1. Check if userLoginId already exists (globally unique)
        if (userLookupPort.existsByUserLoginId(request.userLoginId())) {
            throw new UserLoginIdAlreadyExistsException("User login ID already registered: " + request.userLoginId());
        }

        // 2. Create tenant via gRPC (synchronous provisioning)
        TenantCreationResult tenantResult = tenantServicePort.createTenant(
                request.instituteName(),
                request.businessRegistrationNumber(),
                request.userLoginId(),  // Use userLoginId as owner identifier
                request.name()
        );

        if (!tenantResult.success()) {
            throw new TenantCreationFailedException("Failed to create tenant: " + tenantResult.message());
        }

        log.info("Tenant created: slug={}, tenantId={}", tenantResult.tenantSlug(), tenantResult.tenantId());

        // 3. Hash password and create owner user in public.users
        String passwordHash = passwordEncoder.encode(request.password());
        UserData user = userLookupPort.createUser(
                request.userLoginId(),
                request.name(),
                request.phone(),
                passwordHash,
                Role.OWNER,
                tenantResult.tenantId()
        );

        // 4. Generate tokens
        TokenPair tokenPair = generateTokenPair(user, tenantResult.tenantSlug());

        // 5. Save refresh token to Redis
        saveRefreshToken(user.id(), tenantResult.tenantSlug(), tenantResult.tenantId(), user.role(), tokenPair);

        log.info("Owner registration successful: userId={}, tenantSlug={}", user.id(), tenantResult.tenantSlug());

        return LoginResponse.of(tokenPair, user.toUserResponse(tenantResult.tenantSlug()));
    }

    @Override
    public void logout(String token) {
        log.info("Logout request received");

        TokenClaims claims = jwtTokenProvider.parseToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid token"));

        // Blacklist the token
        long ttl = claims.exp().getEpochSecond() - Instant.now().getEpochSecond();
        if (ttl > 0) {
            tokenPersistencePort.blacklistToken(claims.jti(), ttl);
        }

        // Delete session
        tokenPersistencePort.deleteSession(claims.tenantSlug(), claims.jti());

        // Publish logout event
        eventPublisherPort.publishLogoutEvent(claims.getUserId(), claims.tenantSlug(), false);

        log.info("Logout successful for user: {} on tenant: {}", claims.sub(), claims.tenantSlug());
    }

    @Override
    public void logoutAll(String token) {
        log.info("Logout all sessions request received");

        TokenClaims claims = jwtTokenProvider.parseToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid token"));

        // Blacklist current token
        long ttl = claims.exp().getEpochSecond() - Instant.now().getEpochSecond();
        if (ttl > 0) {
            tokenPersistencePort.blacklistToken(claims.jti(), ttl);
        }

        // Delete all refresh tokens for user
        tokenPersistencePort.deleteAllRefreshTokensForUser(claims.getUserId());

        // Publish logout event
        eventPublisherPort.publishLogoutEvent(claims.getUserId(), claims.tenantSlug(), true);

        log.info("Logout all sessions successful for user: {}", claims.sub());
    }

    @Override
    public TokenResponse refresh(RefreshTokenRequest request) {
        log.info("Token refresh request received");

        // 1. Parse and validate refresh token
        TokenClaims claims = jwtTokenProvider.parseToken(request.refreshToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        // 2. Check if token is blacklisted
        if (tokenPersistencePort.isBlacklisted(claims.jti())) {
            throw new InvalidTokenException("Token has been revoked");
        }

        // 3. Verify refresh token exists in Redis
        tokenPersistencePort.findRefreshToken(claims.getUserId(), claims.jti())
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        // 4. Look up user to get current data from public.users
        UserData user = userLookupPort.findById(claims.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", claims.sub()));

        if (!user.enabled()) {
            throw new IllegalStateException("User account is disabled");
        }

        // 5. Validate tenant is still active
        TenantData tenant = tenantServicePort.validateTenantById(user.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", user.tenantId().toString()));

        if (!tenant.isActive()) {
            throw new IllegalStateException("Tenant is not active");
        }

        // 6. Revoke old refresh token
        tokenPersistencePort.deleteRefreshToken(claims.getUserId(), claims.jti());

        // 7. Generate new token pair
        TokenPair newTokenPair = generateTokenPair(user, tenant.slug());

        // 8. Save new refresh token
        saveRefreshToken(user.id(), tenant.slug(), tenant.id(), user.role(), newTokenPair);

        log.info("Token refresh successful for user: {}", user.id());

        return TokenResponse.from(newTokenPair);
    }

    private TokenPair generateTokenPair(UserData user, String tenantSlug) {
        String accessJti = UUID.randomUUID().toString();
        String refreshJti = UUID.randomUUID().toString();

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.id(), tenantSlug, user.tenantId(), user.role(), accessJti);
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.id(), tenantSlug, user.tenantId(), user.role(), refreshJti);

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

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException(String message) {
            super(message);
        }
    }

    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) {
            super(message);
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
