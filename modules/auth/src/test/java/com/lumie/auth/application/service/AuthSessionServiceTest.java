package com.lumie.auth.application.service;

import com.lumie.auth.application.dto.request.LoginRequest;
import com.lumie.auth.application.dto.response.LoginResponse;
import com.lumie.auth.application.port.out.SessionPersistencePort;
import com.lumie.auth.application.port.out.UserDirectoryPort;
import com.lumie.auth.application.port.out.UserLookupPort;
import com.lumie.auth.application.port.out.UserLookupPort.UserData;
import com.lumie.auth.domain.vo.AuthSession;
import com.lumie.auth.domain.vo.DeviceCategory;
import com.lumie.auth.domain.vo.SessionMetadata;
import com.lumie.common.auth.Role;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.tenant.api.TenantService;
import com.lumie.tenant.api.TenantStatus;
import org.junit.jupiter.api.AfterEach;
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
    @Mock private SessionPersistencePort sessionPersistencePort;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private LoginSessionHelper loginSessionHelper;

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void loginUsesHostTenantAndStoresOnlyOpaqueSession() {
        TenantContextHolder.setTenant("inst-academy");
        TenantService.TenantData tenant = new TenantService.TenantData(
                10L, "inst-academy", "Academy", TenantStatus.ACTIVE, null,
                Instant.now().toString(), Instant.now().toString());
        UserData user = user();
        AuthSession session = session();
        when(tenantService.getTenantBySlug("inst-academy")).thenReturn(Optional.of(tenant));
        when(userLookupPort.findByUserLoginId("student1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hash")).thenReturn(true);
        when(loginSessionHelper.create(eq(user), eq("inst-academy"), eq(10L), any()))
                .thenReturn(session);

        AuthSessionService service = service();
        LoginResponse response = service.login(new LoginRequest("student1", "password123"),
                new SessionMetadata("Chrome / macOS", DeviceCategory.PC));

        assertThat(response.sessionId()).isEqualTo("opaque-session-id");
        assertThat(response.user().tenantSlug()).isEqualTo("inst-academy");
        verify(userDirectoryPort, never()).findByUserLoginId("student1");
    }

    @Test
    void logoutDeletesTheServerSideSession() {
        AuthSession session = session();
        when(sessionPersistencePort.findById("opaque-session-id")).thenReturn(Optional.of(session));

        service().logout("opaque-session-id");

        verify(sessionPersistencePort).delete(session);
    }

    @Test
    void logoutAllDeletesOnlyTheCurrentTenantUsersSessions() {
        AuthSession session = session();

        service().logoutAll(session);

        verify(sessionPersistencePort).deleteAllForUser("inst-academy", 20L);
    }

    private AuthSessionService service() {
        return new AuthSessionService(tenantService, userLookupPort, userDirectoryPort,
                sessionPersistencePort, passwordEncoder, loginSessionHelper);
    }

    private UserData user() {
        return new UserData(20L, "student1", "Student", "01012345678", null, "hash",
                Role.STUDENT, 10L, true, null);
    }

    private AuthSession session() {
        Instant now = Instant.now();
        return new AuthSession("opaque-session-id", 20L, "Student", "inst-academy", 10L,
                Role.STUDENT, "Chrome / macOS", DeviceCategory.PC, now, now.plusSeconds(604800));
    }
}
