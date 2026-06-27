package com.lumie.auth.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.auth.application.dto.request.LoginRequest;
import com.lumie.auth.application.port.out.TokenPersistencePort;
import com.lumie.auth.application.port.out.TokenProviderPort;
import com.lumie.auth.application.port.out.UserDirectoryPort;
import com.lumie.auth.application.port.out.UserLookupPort;
import com.lumie.auth.application.port.out.UserLookupPort.UserData;
import com.lumie.auth.domain.vo.SessionMetadata;
import com.lumie.auth.domain.vo.TokenPair;
import com.lumie.common.auth.Role;
import com.lumie.tenant.api.TenantService;
import com.lumie.tenant.api.TenantService.TenantData;
import com.lumie.tenant.api.TenantStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthSessionServiceTest {

    @Mock private TenantService tenantService;
    @Mock private UserLookupPort userLookupPort;
    @Mock private UserDirectoryPort userDirectoryPort;
    @Mock private TokenPersistencePort tokenPersistencePort;
    @Mock private TokenProviderPort tokenProvider;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private LoginSessionHelper loginSessionHelper;

    private AuthSessionService authSessionService;

    @BeforeEach
    void setUp() {
        authSessionService = new AuthSessionService(
                tenantService,
                userLookupPort,
                userDirectoryPort,
                tokenPersistencePort,
                tokenProvider,
                passwordEncoder,
                new ObjectMapper(),
                loginSessionHelper);
    }

    @Test
    void loginUsesCustomIdTenantBeforeOwnerDirectoryFallback() {
        LoginRequest request = LoginRequest.builder()
                .userLoginId("student1")
                .password("password123")
                .customId("academy")
                .build();
        TenantData tenant = new TenantData(
                10L,
                "inst-academy",
                "Academy",
                TenantStatus.ACTIVE,
                "academy",
                null,
                Instant.now().toString(),
                Instant.now().toString());
        UserData user = new UserData(
                20L,
                "student1",
                "Student",
                "01012345678",
                null,
                "hash",
                Role.STUDENT,
                10L,
                true,
                null);
        TokenPair tokenPair = TokenPair.of("access", "refresh", 3600L, 604800L);

        when(tenantService.getTenantByCustomId("academy")).thenReturn(Optional.of(tenant));
        when(userLookupPort.findByUserLoginId("student1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hash")).thenReturn(true);
        when(loginSessionHelper.issueTokensForLogin(eq(user), eq("inst-academy"), eq(10L), any(SessionMetadata.class)))
                .thenReturn(new LoginSessionHelper.IssuedTokens(tokenPair, "sid", "access-jti", "refresh-jti"));

        var response = authSessionService.login(request, new SessionMetadata(null, null));

        assertThat(response.user().tenantSlug()).isEqualTo("inst-academy");
        verify(userDirectoryPort, never()).findByUserLoginId("student1");
    }
}
