package com.lumie.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.lumie.auth.application.port.out.SessionPersistencePort;
import com.lumie.auth.application.port.out.UserLookupPort;
import com.lumie.auth.domain.vo.AuthSession;
import com.lumie.auth.domain.vo.DeviceCategory;
import com.lumie.common.auth.Role;
import com.lumie.tenant.api.TenantService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthQueryServiceTest {

    @Mock private SessionPersistencePort sessionPersistencePort;
    @Mock private UserLookupPort userLookupPort;
    @Mock private TenantService tenantService;

    @Test
    void activeSessionIsReturnedForAuthentication() {
        AuthSession session = session("session-id", Instant.now().plusSeconds(3600));
        given(sessionPersistencePort.findById("session-id")).willReturn(Optional.of(session));
        AuthQueryService service = new AuthQueryService(sessionPersistencePort, userLookupPort, tenantService);

        assertThat(service.findActiveSession("session-id")).contains(session);

        verify(sessionPersistencePort, never()).delete(session);
    }

    @Test
    void expiredSessionIsDeletedAndRejected() {
        AuthSession session = session("expired-session", Instant.now().minusSeconds(1));
        given(sessionPersistencePort.findById("expired-session")).willReturn(Optional.of(session));
        AuthQueryService service = new AuthQueryService(sessionPersistencePort, userLookupPort, tenantService);

        assertThat(service.findActiveSession("expired-session")).isEmpty();

        verify(sessionPersistencePort).delete(session);
    }

    @Test
    void unknownSessionIsRejected() {
        given(sessionPersistencePort.findById("missing-session")).willReturn(Optional.empty());
        AuthQueryService service = new AuthQueryService(sessionPersistencePort, userLookupPort, tenantService);

        assertThat(service.findActiveSession("missing-session")).isEmpty();

        verify(sessionPersistencePort, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    private static AuthSession session(String id, Instant expiresAt) {
        return new AuthSession(id, 42L, "User", "academy", 7L, Role.STUDENT,
                "Chrome", DeviceCategory.PC, Instant.now().minusSeconds(60), expiresAt);
    }
}
