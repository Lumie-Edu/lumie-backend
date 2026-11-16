package com.lumie.auth.application.service;

import com.lumie.auth.adapter.out.config.SessionConfig;
import com.lumie.auth.application.port.out.SessionPersistencePort;
import com.lumie.auth.application.port.out.UserLookupPort.UserData;
import com.lumie.auth.domain.vo.AuthSession;
import com.lumie.auth.domain.vo.DeviceCategory;
import com.lumie.auth.domain.vo.SessionMetadata;
import com.lumie.common.auth.Role;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class LoginSessionHelperTest {

    private static final String TENANT_SLUG = "academy-a";
    private static final Long USER_ID = 8L;

    @Mock private SessionPersistencePort sessions;
    @Mock private SessionConfig sessionConfig;
    @InjectMocks private LoginSessionHelper helper;

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void createsUnknownCategorySessionWithoutLookingUpExistingSessions() {
        given(sessionConfig.getExpiration()).willReturn(600L);

        AuthSession session = helper.create(owner(), TENANT_SLUG, 22L, new SessionMetadata(null, null));

        assertThat(session.id()).isNotBlank();
        assertThat(session.id()).matches("[A-Za-z0-9_-]{43}");
        assertThat(session.device()).isEqualTo("Unknown");
        assertThat(session.deviceCategory()).isEqualTo(DeviceCategory.UNKNOWN);
        assertThat(session.expiresAt()).isEqualTo(session.createdAt().plusSeconds(600));
        verify(sessions, never()).findSessionIds(TENANT_SLUG, USER_ID);
        verify(sessions).save(session);
    }

    @Test
    void skipsRollbackSynchronizationWhenTransactionHasNoSynchronization() {
        given(sessionConfig.getExpiration()).willReturn(600L);
        TransactionSynchronizationManager.setActualTransactionActive(true);

        AuthSession session = helper.create(owner(), TENANT_SLUG, 22L,
                new SessionMetadata("Browser", DeviceCategory.UNKNOWN));

        assertThat(session).isNotNull();
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    }

    @Test
    void replacesExistingSessionForSameDeviceCategory() {
        AuthSession existing = existingSession(DeviceCategory.PC);
        given(sessionConfig.getExpiration()).willReturn(600L);
        given(sessions.findSessionIds(TENANT_SLUG, USER_ID)).willReturn(Set.of("old-session"));
        given(sessions.findSessions(TENANT_SLUG, USER_ID, Set.of("old-session")))
                .willReturn(Map.of("old-session", existing));

        helper.create(owner(), TENANT_SLUG, 22L, new SessionMetadata("Browser", DeviceCategory.PC));

        verify(sessions).delete(existing);
    }

    @Test
    void retainsExistingSessionWhenDeviceCategoryDiffers() {
        AuthSession existing = existingSession(DeviceCategory.MOBILE);
        given(sessionConfig.getExpiration()).willReturn(600L);
        given(sessions.findSessionIds(TENANT_SLUG, USER_ID)).willReturn(Set.of("old-session"));
        given(sessions.findSessions(TENANT_SLUG, USER_ID, Set.of("old-session")))
                .willReturn(Map.of("old-session", existing));

        helper.create(owner(), TENANT_SLUG, 22L, new SessionMetadata("Browser", DeviceCategory.PC));

        verify(sessions, never()).delete(existing);
    }

    @Test
    void rollbackDeletesTheSessionWrittenToRedis() {
        given(sessionConfig.getExpiration()).willReturn(600L);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        AuthSession session = helper.create(owner(), TENANT_SLUG, 22L,
                new SessionMetadata("Browser", DeviceCategory.UNKNOWN));
        TransactionSynchronization synchronization = TransactionSynchronizationManager.getSynchronizations().getFirst();
        synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(sessions).delete(session);
    }

    @Test
    void commitKeepsTheSessionWrittenToRedis() {
        given(sessionConfig.getExpiration()).willReturn(600L);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        AuthSession session = helper.create(owner(), TENANT_SLUG, 22L,
                new SessionMetadata("Browser", DeviceCategory.UNKNOWN));
        TransactionSynchronizationManager.getSynchronizations().getFirst()
                .afterCompletion(TransactionSynchronization.STATUS_COMMITTED);

        verify(sessions, never()).delete(session);
    }

    @Test
    void rollbackCleanupFailureDoesNotEscapeCompletionCallback() {
        given(sessionConfig.getExpiration()).willReturn(600L);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        doThrow(new IllegalStateException("redis unavailable"))
                .when(sessions).delete(org.mockito.ArgumentMatchers.any());

        AuthSession session = helper.create(owner(), TENANT_SLUG, 22L,
                new SessionMetadata("Browser", DeviceCategory.UNKNOWN));
        TransactionSynchronizationManager.getSynchronizations().getFirst()
                .afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(sessions).delete(session);
    }

    private UserData owner() {
        return new UserData(USER_ID, "owner-login", "Owner", null, null, "hash", Role.OWNER,
                22L, true, null);
    }

    private AuthSession existingSession(DeviceCategory category) {
        return new AuthSession("old-session", USER_ID, "Owner", TENANT_SLUG, 22L,
                Role.OWNER, "Browser", category, java.time.Instant.EPOCH, java.time.Instant.EPOCH.plusSeconds(600));
    }
}
