package com.lumie.auth.adapter.in.internal;

import com.lumie.auth.application.port.out.SessionPersistencePort;
import com.lumie.auth.application.port.out.UserLookupPort;
import com.lumie.common.auth.Role;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthServiceAdapterRoleLookupTest {
    @Mock private UserLookupPort users;
    @Mock private PasswordEncoder passwords;
    @Mock private SessionPersistencePort sessions;
    @InjectMocks private AuthServiceAdapter adapter;

    @Test
    void getUserRolesDelegatesTheBatchLookup() {
        given(users.getUserRoles(List.of(1L, 2L))).willReturn(Map.of(1L, "STUDENT"));

        assertThat(adapter.getUserRoles(List.of(1L, 2L)))
                .isEqualTo(Map.of(1L, "STUDENT"));
    }

    @Test
    void getTenantOwnerMapsTheOwnerLookupResult() {
        given(users.findTenantOwner()).willReturn(Optional.of(new UserLookupPort.UserData(
                8L, "owner-login", "Academy Owner", "01012345678", "owner@example.com", "hash",
                Role.OWNER, 22L, true, "seed", Instant.EPOCH, Instant.EPOCH)));

        assertThat(adapter.getTenantOwner()).contains(new com.lumie.auth.api.AuthService.TenantOwnerData(
                8L, "owner-login", "Academy Owner", "01012345678", "owner@example.com", "seed",
                Instant.EPOCH, Instant.EPOCH));
    }
}
