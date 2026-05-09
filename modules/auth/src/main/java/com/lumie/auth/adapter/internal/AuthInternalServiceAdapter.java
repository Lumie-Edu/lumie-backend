package com.lumie.auth.adapter.internal;

import com.lumie.auth.adapter.out.persistence.UserJpaRepository;
import com.lumie.auth.application.port.in.ValidateTokenUseCase;
import com.lumie.auth.application.port.out.UserLookupPort;
import com.lumie.common.auth.Role;
import com.lumie.internal.auth.AuthInternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInternalServiceAdapter implements AuthInternalService {

    private static final String DEFAULT_PASSWORD = "0000!Lumie";
    private static final int MAX_LOGIN_ID_ATTEMPTS = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ValidateTokenUseCase validateTokenUseCase;
    private final UserLookupPort userLookupPort;
    private final UserJpaRepository userJpaRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Map<Long, String> getAvatarSeeds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userJpaRepository.findAvatarSeedsByIds(userIds).stream()
                .collect(Collectors.toMap(
                        UserJpaRepository.UserAvatarRow::getId,
                        UserJpaRepository.UserAvatarRow::getAvatarSeed,
                        (a, b) -> a));
    }

    @Override
    public TokenValidationResult validateToken(String token) {
        Optional<com.lumie.auth.domain.vo.TokenClaims> claimsOpt = validateTokenUseCase.validate(token);

        if (claimsOpt.isPresent()) {
            var claims = claimsOpt.get();
            return new TokenValidationResult(true, "Token is valid",
                    new TokenClaims(
                            claims.sub(), claims.tenantSlug(), claims.tenantId(),
                            claims.role().name(),
                            claims.iat().getEpochSecond(), claims.exp().getEpochSecond(),
                            claims.jti()
                    ));
        }
        return new TokenValidationResult(false, "Token is invalid or expired", null);
    }

    @Override
    public Optional<UserData> getUserInfo(String token) {
        Optional<com.lumie.auth.domain.vo.TokenClaims> claimsOpt = validateTokenUseCase.validate(token);
        if (claimsOpt.isEmpty()) {
            return Optional.empty();
        }

        var claims = claimsOpt.get();
        var userOpt = userLookupPort.findById(claims.getUserId());
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        var user = userOpt.get();
        return Optional.of(new UserData(
                user.id(), user.userLoginId(), user.name(),
                user.role().name(), claims.tenantSlug(), claims.tenantId()
        ));
    }

    @Override
    public CreateUserResult createUser(String name, String phone, String role, Long tenantId) {
        // The caller (admin-svc / student-svc) is already running in the right
        // tenant context. Validate that the explicit tenantId argument matches
        // it — mismatch would route the new user into the wrong schema.
        Long contextTenantId = com.lumie.common.tenant.TenantContextHolder.getTenantId();
        if (contextTenantId == null || !contextTenantId.equals(tenantId)) {
            return new CreateUserResult(false,
                    "Tenant context mismatch (context=" + contextTenantId + ", request=" + tenantId + ")",
                    null, null);
        }
        try {
            Role userRole = Role.valueOf(role);
            String userLoginId = generateUniqueLoginId();
            String passwordHash = passwordEncoder.encode(DEFAULT_PASSWORD);
            UserLookupPort.UserData user = userLookupPort.createUser(
                    userLoginId, name, phone, passwordHash, userRole);
            return new CreateUserResult(true, "User created successfully", user.id(), user.userLoginId());
        } catch (IllegalArgumentException e) {
            return new CreateUserResult(false, "Invalid role: " + role, null, null);
        }
    }

    private String generateUniqueLoginId() {
        for (int i = 0; i < MAX_LOGIN_ID_ATTEMPTS; i++) {
            String loginId = "s" + String.format("%08d", RANDOM.nextInt(100_000_000));
            if (!userLookupPort.existsByUserLoginId(loginId)) {
                return loginId;
            }
        }
        throw new IllegalStateException("Failed to generate unique login ID after " + MAX_LOGIN_ID_ATTEMPTS + " attempts");
    }

    @Override
    public DeleteUserResult deleteUser(Long userId) {
        boolean deleted = userLookupPort.deleteUser(userId);
        if (deleted) {
            return new DeleteUserResult(true, "User deleted successfully");
        }
        return new DeleteUserResult(false, "User not found: " + userId);
    }

    @Override
    public ResetPasswordResult resetPassword(Long userId) {
        String newPasswordHash = passwordEncoder.encode(DEFAULT_PASSWORD);
        boolean updated = userLookupPort.resetPassword(userId, newPasswordHash);
        if (updated) {
            return new ResetPasswordResult(true, "Password reset successfully");
        }
        return new ResetPasswordResult(false, "User not found: " + userId);
    }

    @Override
    public ChangeLoginIdResult changeLoginId(Long userId, String newLoginId) {
        var result = userLookupPort.changeLoginId(userId, newLoginId);
        return new ChangeLoginIdResult(result.success(), result.message(), result.newLoginId());
    }
}
