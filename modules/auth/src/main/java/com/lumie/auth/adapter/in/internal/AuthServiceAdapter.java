package com.lumie.auth.adapter.in.internal;

import com.lumie.auth.application.exception.AuthErrors;
import com.lumie.auth.application.port.out.SessionPersistencePort;
import com.lumie.auth.application.port.out.UserLookupPort;
import com.lumie.common.auth.Role;
import com.lumie.common.util.PhoneNumberUtils;
import com.lumie.auth.api.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthServiceAdapter implements AuthService {

    private static final String DEFAULT_PASSWORD = "0000!Lumie";
    private static final int MAX_LOGIN_ID_ATTEMPTS = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserLookupPort userLookupPort;
    private final PasswordEncoder passwordEncoder;
    private final SessionPersistencePort sessionPersistencePort;

    @Override
    public Map<Long, String> getAvatarSeeds(Collection<Long> userIds) {
        return userLookupPort.getAvatarSeeds(userIds);
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

            String userLoginId;
            if (phone != null && !phone.isBlank()) {
                userLoginId = PhoneNumberUtils.toDigits(phone);
                if (userLookupPort.existsByUserLoginId(userLoginId)) {
                    return new CreateUserResult(false, "Login ID (phone) already exists: " + userLoginId, null, null);
                }
            } else {
                userLoginId = generateUniqueLoginId();
            }

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
        throw new AuthErrors.LoginIdGenerationFailedException();
    }

    @Override
    public boolean updateIdentity(Long userId, String name, String phone, String email) {
        return userLookupPort.updateIdentity(userId, name, phone, email);
    }

    @Override
    public DeleteUserResult deleteUser(Long userId) {
        boolean deleted = userLookupPort.deleteUser(userId);
        if (deleted) {
            revokeTenantSessions(userId);
            return new DeleteUserResult(true, "User deleted successfully");
        }
        return new DeleteUserResult(false, "User not found: " + userId);
    }

    @Override
    public ResetPasswordResult resetPassword(Long userId) {
        String newPasswordHash = passwordEncoder.encode(DEFAULT_PASSWORD);
        boolean updated = userLookupPort.resetPassword(userId, newPasswordHash);
        if (updated) {
            revokeTenantSessions(userId);
            return new ResetPasswordResult(true, "Password reset successfully");
        }
        return new ResetPasswordResult(false, "User not found: " + userId);
    }

    @Override
    public ChangeLoginIdResult changeLoginId(Long userId, String newLoginId) {
        var result = userLookupPort.changeLoginId(userId, newLoginId);
        return new ChangeLoginIdResult(result.success(), result.message(), result.newLoginId());
    }

    private void revokeTenantSessions(Long userId) {
        String tenantSlug = com.lumie.common.tenant.TenantContextHolder.getTenant();
        if (tenantSlug != null) {
            sessionPersistencePort.deleteAllForUser(tenantSlug, userId);
        }
    }
}
