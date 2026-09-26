package com.lumie.auth.adapter.in.security;

import com.lumie.auth.adapter.in.web.CookieUtils;
import com.lumie.auth.adapter.out.config.SessionConfig;
import com.lumie.auth.application.port.out.UserLookupPort;
import com.lumie.auth.application.service.LoginSessionHelper;
import com.lumie.auth.domain.vo.AuthSession;
import com.lumie.auth.domain.vo.DeviceCategory;
import com.lumie.common.auth.Role;
import com.lumie.common.tenant.TenantContextHolder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyJwtMigrationFilterTest {
    private static final String SECRET = "legacy-session-migration-test-secret-must-be-at-least-32-bytes";
    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> values;
    @Mock private UserLookupPort users;
    @Mock private LoginSessionHelper sessions;
    @Mock private CookieUtils cookieUtils;

    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @Test
    void exchangesOnlyAValidStoredRefreshTokenForOpaqueSession() throws Exception {
        TenantContextHolder.setTenant("joossameng");
        TenantContextHolder.setTenantId(7L);
        String token = refreshToken();
        MockHttpServletRequest request = request(token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        UserLookupPort.UserData user = new UserLookupPort.UserData(10L, "owner", "Owner", null, null,
                "hash", Role.OWNER, 7L, true, null);
        AuthSession session = new AuthSession("opaque", 10L, "Owner", "joossameng", 7L, Role.OWNER,
                "Chrome / macOS", DeviceCategory.PC, Instant.now(), Instant.now().plusSeconds(604800));
        when(redis.hasKey("auth:blacklist:refresh-jti")).thenReturn(false);
        when(redis.opsForValue()).thenReturn(values);
        when(redis.hasKey("auth:session-revoked:joossameng:legacy-sid")).thenReturn(false);
        when(redis.hasKey("auth:refresh:10:refresh-jti")).thenReturn(true);
        when(users.findById(10L)).thenReturn(Optional.of(user));
        when(sessions.create(eq(user), eq("joossameng"), eq(7L), any())).thenReturn(session);
        when(cookieUtils.createSessionCookie("opaque")).thenReturn(org.springframework.http.ResponseCookie.from("__Host-lumie_session", "opaque").build());

        filter().doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("X-Lumie-Migrated-Path")).isEqualTo("/admin");
        assertThat(response.getHeaders("Set-Cookie")).anyMatch(header -> header.contains("__Host-lumie_session=opaque"));
    }

    @Test
    void refusesARefreshTokenThatIsNotStoredInLegacyRedis() throws Exception {
        TenantContextHolder.setTenant("joossameng");
        TenantContextHolder.setTenantId(7L);
        when(redis.hasKey("auth:blacklist:refresh-jti")).thenReturn(false);
        when(redis.hasKey("auth:session-revoked:joossameng:legacy-sid")).thenReturn(false);
        when(redis.hasKey("auth:refresh:10:refresh-jti")).thenReturn(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter().doFilter(request(refreshToken()), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(204);
    }

    private LegacyJwtMigrationFilter filter() {
        LegacyJwtMigrationFilter filter = new LegacyJwtMigrationFilter(redis, users, sessions, cookieUtils, new SessionConfig());
        ReflectionTestUtils.setField(filter, "secret", SECRET);
        ReflectionTestUtils.setField(filter, "sunsetAt", Instant.now().plusSeconds(60));
        return filter;
    }

    private MockHttpServletRequest request(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/auth/migrate-legacy-session");
        request.addHeader("Host", "joossameng.localhost:3000");
        request.addHeader("Origin", "http://joossameng.localhost:3000");
        request.setCookies(new jakarta.servlet.http.Cookie("lumie_refresh_token", token));
        return request;
    }

    private String refreshToken() {
        Instant now = Instant.now();
        return Jwts.builder().subject("10").issuer("lumie-auth-svc").id("refresh-jti")
                .claim("type", "refresh").claim("tenant_slug", "joossameng").claim("tenant_id", 7L)
                .claim("sid", "legacy-sid").issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }
}
