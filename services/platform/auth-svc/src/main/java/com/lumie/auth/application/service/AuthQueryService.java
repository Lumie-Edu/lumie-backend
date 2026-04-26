package com.lumie.auth.application.service;

import com.lumie.auth.application.dto.response.UserResponse;
import com.lumie.auth.application.port.in.GetMeUseCase;
import com.lumie.auth.application.port.in.ValidateTokenUseCase;
import com.lumie.auth.application.port.out.TokenPersistencePort;
import com.lumie.auth.application.port.out.UserLookupPort;
import com.lumie.auth.application.port.out.UserLookupPort.UserData;
import com.lumie.auth.domain.vo.TokenClaims;
import com.lumie.auth.infrastructure.security.JwtTokenProvider;
import com.lumie.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthQueryService implements ValidateTokenUseCase, GetMeUseCase {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenPersistencePort tokenPersistencePort;
    private final UserLookupPort userLookupPort;

    @Override
    public Optional<TokenClaims> validate(String token) {
        log.debug("Validating token");

        // 1. Parse and validate JWT
        Optional<TokenClaims> claimsOpt = jwtTokenProvider.parseToken(token);
        if (claimsOpt.isEmpty()) {
            log.debug("Token parsing failed");
            return Optional.empty();
        }

        TokenClaims claims = claimsOpt.get();

        // 2. Check if token is blacklisted
        if (tokenPersistencePort.isBlacklisted(claims.jti())) {
            log.debug("Token is blacklisted: {}", claims.jti());
            return Optional.empty();
        }

        // 3. Check if token is expired
        if (claims.isExpired()) {
            log.debug("Token is expired");
            return Optional.empty();
        }

        log.debug("Token validated successfully for user: {}", claims.sub());
        return Optional.of(claims);
    }

    @Override
    public boolean isValid(String token) {
        return validate(token).isPresent();
    }

    @Override
    public UserResponse getMe(Long userId, String tenantSlug) {
        UserData user = userLookupPort.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", String.valueOf(userId)));
        return user.toUserResponse(tenantSlug);
    }
}
