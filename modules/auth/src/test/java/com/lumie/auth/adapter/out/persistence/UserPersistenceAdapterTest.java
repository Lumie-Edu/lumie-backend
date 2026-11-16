package com.lumie.auth.adapter.out.persistence;

import com.lumie.common.auth.Role;
import com.lumie.auth.domain.entity.User;
import com.lumie.common.tenant.TenantContextHolder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UserPersistenceAdapterTest {
    @Mock private UserJpaRepository repository;
    @Mock private UserJpaRepository.UserRoleRow student;
    @Mock private UserJpaRepository.UserRoleRow owner;
    @InjectMocks private UserPersistenceAdapter adapter;

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

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

    @Test
    void findTenantOwnerReturnsTheOwnerFromTheCurrentTenant() {
        TenantContextHolder.setTenantId(22L);
        User owner = User.create("owner-login", "Academy Owner", "01012345678", "hash", Role.OWNER);
        ReflectionTestUtils.setField(owner, "id", 8L);
        given(repository.findFirstByRoleOrderByIdAsc(Role.OWNER)).willReturn(Optional.of(owner));

        assertThat(adapter.findTenantOwner()).get().satisfies(result -> {
            assertThat(result.id()).isEqualTo(8L);
            assertThat(result.role()).isEqualTo(Role.OWNER);
            assertThat(result.tenantId()).isEqualTo(22L);
            assertThat(result.enabled()).isTrue();
            assertThat(result.avatarSeed()).isEqualTo(owner.getAvatarSeed());
        });
    }
}
