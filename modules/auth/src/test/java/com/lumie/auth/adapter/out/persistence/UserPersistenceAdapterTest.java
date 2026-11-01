package com.lumie.auth.adapter.out.persistence;

import com.lumie.common.auth.Role;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UserPersistenceAdapterTest {
    @Mock private UserJpaRepository repository;
    @Mock private UserJpaRepository.UserRoleRow student;
    @Mock private UserJpaRepository.UserRoleRow owner;
    @InjectMocks private UserPersistenceAdapter adapter;

    @Test
    void getUserRolesSkipsLookupForEmptyIds() {
        assertThat(adapter.getUserRoles(List.of())).isEmpty();
        assertThat(adapter.getUserRoles(null)).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    void getUserRolesMapsExistingUsersInOneLookup() {
        given(student.getId()).willReturn(1L);
        given(student.getRole()).willReturn(Role.STUDENT);
        given(owner.getId()).willReturn(2L);
        given(owner.getRole()).willReturn(Role.OWNER);
        given(repository.findRolesByIds(List.of(1L, 2L, 3L))).willReturn(List.of(student, owner));

        assertThat(adapter.getUserRoles(List.of(1L, 2L, 3L)))
                .isEqualTo(Map.of(1L, "STUDENT", 2L, "OWNER"));
    }
}
