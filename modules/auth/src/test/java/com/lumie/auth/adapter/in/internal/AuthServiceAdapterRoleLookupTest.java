package com.lumie.auth.adapter.in.internal;

import com.lumie.auth.application.port.out.SessionPersistencePort;
import com.lumie.auth.application.port.out.UserLookupPort;
import java.util.List;
import java.util.Map;
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
}
