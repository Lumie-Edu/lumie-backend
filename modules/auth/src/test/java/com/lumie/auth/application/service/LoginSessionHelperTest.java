package com.lumie.auth.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.auth.application.port.out.TokenPersistencePort;
import com.lumie.auth.application.port.out.TokenProviderPort;
import com.lumie.auth.application.port.out.UserLookupPort.UserData;
import com.lumie.auth.domain.vo.DeviceCategory;
import com.lumie.auth.domain.vo.SessionMetadata;
import com.lumie.auth.domain.vo.SessionRevocationReason;
import com.lumie.auth.domain.vo.TokenClaims;
import com.lumie.common.auth.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginSessionHelperTest {

    @Mock private TokenPersistencePort tokenPersistencePort;
    @Mock private TokenProviderPort tokenProvider;

    private LoginSessionHelper loginSessionHelper;

    @BeforeEach
    void setUp() {
        loginSessionHelper = new LoginSessionHelper(tokenPersistencePort, tokenProvider, new ObjectMapper());
    }

    @Test
    void issueTokensForLoginStoresRevocationReasonWhenReplacingSameDeviceCategorySession() {
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
        String oldSessionData = """
                {"userId":20,"device":"Chrome","deviceCategory":"PC","createdAt":1,"accessJti":"old-access","refreshJti":"old-refresh"}
                """;
        Instant now = Instant.now();
        TokenClaims accessClaims = TokenClaims.of(
                "20", "Student", "inst-academy", 10L, Role.STUDENT, now, now.plusSeconds(3600), "new-access", "new-sid");
        TokenClaims refreshClaims = TokenClaims.of(
                "20", "Student", "inst-academy", 10L, Role.STUDENT, now, now.plusSeconds(604800), "new-refresh", "new-sid");

        when(tokenPersistencePort.findAllSessionJtis("inst-academy", 20L)).thenReturn(Set.of("old-sid"));
        when(tokenPersistencePort.findSessionsBulk("inst-academy", Set.of("old-sid")))
                .thenReturn(Map.of("old-sid", oldSessionData));
        when(tokenProvider.getAccessTokenExpiration()).thenReturn(3600L);
        when(tokenProvider.getRefreshTokenExpiration()).thenReturn(604800L);
        when(tokenProvider.generateAccessToken(eq(20L), eq("Student"), eq("inst-academy"), eq(10L), eq(Role.STUDENT), anyString(), anyString()))
                .thenReturn("access");
        when(tokenProvider.generateRefreshToken(eq(20L), eq("Student"), eq("inst-academy"), eq(10L), eq(Role.STUDENT), anyString(), anyString()))
                .thenReturn("refresh");
        when(tokenProvider.parseToken("access")).thenReturn(java.util.Optional.of(accessClaims));
        when(tokenProvider.parseToken("refresh")).thenReturn(java.util.Optional.of(refreshClaims));

        loginSessionHelper.issueTokensForLogin(
                user,
                "inst-academy",
                10L,
                new SessionMetadata("Chrome", DeviceCategory.PC));

        verify(tokenPersistencePort).saveSessionRevocationReason(
                "inst-academy",
                "old-sid",
                SessionRevocationReason.SESSION_REPLACED_BY_LOGIN,
                604800L);
    }
}
