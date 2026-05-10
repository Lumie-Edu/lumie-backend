package com.lumie.auth.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.auth.application.dto.response.SessionResponse;
import com.lumie.auth.application.dto.response.UserResponse;
import com.lumie.auth.application.port.out.TokenPersistencePort;
import com.lumie.auth.application.port.out.UserLookupPort;
import com.lumie.auth.application.port.out.UserLookupPort.UserData;
import com.lumie.auth.domain.vo.TokenClaims;
import com.lumie.auth.infrastructure.security.JwtTokenProvider;
import com.lumie.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthQueryService {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenPersistencePort tokenPersistencePort;
    private final UserLookupPort userLookupPort;
    private final ObjectMapper objectMapper;

    public Optional<TokenClaims> validate(String token) {
        log.debug("Validating token");

        Optional<TokenClaims> claimsOpt = jwtTokenProvider.parseToken(token);
        if (claimsOpt.isEmpty()) {
            log.debug("Token parsing failed");
            return Optional.empty();
        }

        TokenClaims claims = claimsOpt.get();

        if (tokenPersistencePort.isBlacklisted(claims.jti())) {
            log.debug("Token is blacklisted: {}", claims.jti());
            return Optional.empty();
        }

        if (claims.isExpired()) {
            log.debug("Token is expired");
            return Optional.empty();
        }

        log.debug("Token validated successfully for user: {}", claims.sub());
        return Optional.of(claims);
    }

    public boolean isValid(String token) {
        return validate(token).isPresent();
    }

    public UserResponse getMe(Long userId, String tenantSlug) {
        UserData user = userLookupPort.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", String.valueOf(userId)));
        return user.toUserResponse(tenantSlug);
    }

    public List<SessionResponse> getSessions(Long userId, String tenantSlug, String currentSid) {
        Set<String> sids = tokenPersistencePort.findAllSessionJtis(tenantSlug, userId);
        if (sids.isEmpty()) {
            return List.of();
        }

        Map<String, String> sessionDataMap = tokenPersistencePort.findSessionsBulk(tenantSlug, sids);

        List<String> staleSids = new ArrayList<>();
        for (String sid : sids) {
            if (!sessionDataMap.containsKey(sid)) {
                staleSids.add(sid);
            }
        }
        for (String staleSid : staleSids) {
            tokenPersistencePort.removeSessionFromIndex(tenantSlug, userId, staleSid);
        }

        List<SessionResponse> sessions = new ArrayList<>();
        for (Map.Entry<String, String> entry : sessionDataMap.entrySet()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = objectMapper.readValue(entry.getValue(), Map.class);
                String device = (String) data.getOrDefault("device", "Unknown");
                String deviceCategory = (String) data.getOrDefault("deviceCategory", "UNKNOWN");
                long createdAt = ((Number) data.getOrDefault("createdAt", 0L)).longValue();
                String sid = entry.getKey();
                boolean isCurrent = currentSid != null && currentSid.equals(sid);

                sessions.add(new SessionResponse(sid, device, deviceCategory, createdAt, isCurrent));
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize session data for sid: {}", entry.getKey(), e);
            }
        }

        sessions.sort(Comparator.comparingLong(SessionResponse::createdAt).reversed());
        return sessions;
    }
}
