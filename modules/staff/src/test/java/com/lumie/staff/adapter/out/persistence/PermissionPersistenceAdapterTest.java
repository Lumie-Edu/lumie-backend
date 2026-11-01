package com.lumie.staff.adapter.out.persistence;

import com.lumie.staff.domain.entity.Permission;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class PermissionPersistenceAdapterTest {
    private final PermissionJpaRepository repository = mock(PermissionJpaRepository.class);
    private final PermissionPersistenceAdapter adapter = new PermissionPersistenceAdapter(repository);

    @Test
    void delegatesAllQueriesAndPreservesEmptyResults() {
        Permission permission = Permission.create("STAFF_READ", "Read", "STAFF", null);
        when(repository.findAll()).thenReturn(List.of(permission));
        when(repository.findByCategory("STAFF")).thenReturn(List.of());
        when(repository.findByIdIn(Set.of(1L))).thenReturn(Set.of(permission));
        when(repository.findDistinctCategories()).thenReturn(List.of("STAFF", "STUDENT"));

        assertThat(adapter.findAll()).containsExactly(permission);
        assertThat(adapter.findByCategory("STAFF")).isEmpty();
        assertThat(adapter.findByIdIn(Set.of(1L))).containsExactly(permission);
        assertThat(adapter.findDistinctCategories()).containsExactly("STAFF", "STUDENT");
        verify(repository).findByCategory("STAFF");
        verify(repository).findByIdIn(Set.of(1L));
        verify(repository).findDistinctCategories();
    }

    @Test
    void preservesStorageFailures() {
        IllegalStateException failure = new IllegalStateException("database unavailable");
        when(repository.findDistinctCategories()).thenThrow(failure);
        assertThatThrownBy(adapter::findDistinctCategories).isSameAs(failure);
    }
}
