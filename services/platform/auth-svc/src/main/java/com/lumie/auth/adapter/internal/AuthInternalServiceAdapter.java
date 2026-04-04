package com.lumie.auth.adapter.internal;

import com.lumie.auth.application.port.in.ValidateTokenUseCase;
import com.lumie.auth.application.port.out.UserLookupPort;
import com.lumie.auth.domain.vo.Role;
import com.lumie.internal.auth.AuthInternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInternalServiceAdapter implements AuthInternalService {

    private final ValidateTokenUseCase validateTokenUseCase;
    private final UserLookupPort userLookupPort;
    private final PasswordEncoder passwordEncoder;

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
    public CreateUserResult createUser(String userLoginId, String password, String name,
                                       String phone, String role, Long tenantId) {
        if (userLookupPort.existsByUserLoginId(userLoginId)) {
            return new CreateUserResult(false, "User login ID already exists: " + userLoginId, null);
        }

        try {
            String passwordHash = passwordEncoder.encode(password);
            Role userRole = Role.valueOf(role);
            UserLookupPort.UserData user = userLookupPort.createUser(
                    userLoginId, name, phone, passwordHash, userRole, tenantId);
            return new CreateUserResult(true, "User created successfully", user.id());
        } catch (IllegalArgumentException e) {
            return new CreateUserResult(false, "Invalid role: " + role, null);
        }
    }

    @Override
    public DeleteUserResult deleteUser(Long userId) {
        boolean deleted = userLookupPort.deleteUser(userId);
        if (deleted) {
            return new DeleteUserResult(true, "User deleted successfully");
        }
        return new DeleteUserResult(false, "User not found: " + userId);
    }
}
