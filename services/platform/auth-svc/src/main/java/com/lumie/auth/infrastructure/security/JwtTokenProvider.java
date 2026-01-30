package com.lumie.auth.infrastructure.security;

import com.lumie.auth.domain.vo.Role;
import com.lumie.auth.domain.vo.TokenClaims;
import com.lumie.auth.infrastructure.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtConfig jwtConfig;
    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(jwtConfig.getSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, String tenantSlug, Long tenantId, Role role, String jti) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(jwtConfig.getAccessExpiration());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("tenant_slug", tenantSlug)
                .claim("tenant_id", tenantId)
                .claim("role", role.name())
                .claim("type", "access")
                .id(jti)
                .issuer(jwtConfig.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(Long userId, String tenantSlug, Long tenantId, Role role, String jti) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(jwtConfig.getRefreshExpiration());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("tenant_slug", tenantSlug)
                .claim("tenant_id", tenantId)
                .claim("role", role.name())
                .claim("type", "refresh")
                .id(jti)
                .issuer(jwtConfig.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    public Optional<TokenClaims> parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Optional.of(TokenClaims.of(
                    claims.getSubject(),
                    claims.get("tenant_slug", String.class),
                    claims.get("tenant_id", Long.class),
                    Role.fromString(claims.get("role", String.class)),
                    claims.getIssuedAt().toInstant(),
                    claims.getExpiration().toInstant(),
                    claims.getId()
            ));
        } catch (JwtException e) {
            log.debug("Failed to parse JWT: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public boolean validateToken(String token) {
        return parseToken(token).isPresent();
    }

    public long getAccessTokenExpiration() {
        return jwtConfig.getAccessExpiration();
    }

    public long getRefreshTokenExpiration() {
        return jwtConfig.getRefreshExpiration();
    }
}
