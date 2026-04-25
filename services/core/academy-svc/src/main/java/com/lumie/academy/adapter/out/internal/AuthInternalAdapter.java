package com.lumie.academy.adapter.out.internal;

import com.lumie.academy.application.port.out.AuthServicePort;
import com.lumie.internal.auth.AuthInternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component("academyAuthInternalAdapter")
@RequiredArgsConstructor
public class AuthInternalAdapter implements AuthServicePort {

    private final AuthInternalService authInternalService;

    @Override
    public Optional<TokenClaimsData> validateToken(String token) {
        log.debug("Validating token via internal API");
        var result = authInternalService.validateToken(token);
        if (!result.valid() || result.claims() == null) {
            log.debug("Token validation failed: {}", result.message());
            return Optional.empty();
        }
        var claims = result.claims();
        try {
            return Optional.of(new TokenClaimsData(
                    Long.parseLong(claims.sub()),
                    claims.tenantSlug(),
                    claims.tenantId(),
                    claims.role()
            ));
        } catch (NumberFormatException e) {
            log.error("Failed to parse user ID from token claims", e);
            return Optional.empty();
        }
    }

    @Override
    public CreateUserResult createUser(CreateUserRequest request) {
        log.debug("Creating user via internal API: name={}, role={}", request.name(), request.role());
        var result = authInternalService.createUser(
                request.name(),
                request.phone(),
                request.role(),
                request.tenantId()
        );
        return new CreateUserResult(result.success(), result.message(), result.userId(), result.userLoginId());
    }

    @Override
    public DeleteUserResult deleteUser(Long userId) {
        log.debug("Deleting user via internal API: {}", userId);
        var result = authInternalService.deleteUser(userId);
        return new DeleteUserResult(result.success(), result.message());
    }

    @Override
    public ResetPasswordResult resetPassword(Long userId) {
        log.debug("Resetting password via internal API: {}", userId);
        var result = authInternalService.resetPassword(userId);
        return new ResetPasswordResult(result.success(), result.message());
    }

    @Override
    public ChangeLoginIdResult changeLoginId(Long userId, String newLoginId) {
        log.debug("Changing login ID via internal API: {}", userId);
        var result = authInternalService.changeLoginId(userId, newLoginId);
        return new ChangeLoginIdResult(result.success(), result.message(), result.newLoginId());
    }
}
