package com.lumie.staff.adapter.out.persistence;

import com.lumie.staff.domain.entity.StaffPermission;
import com.lumie.staff.domain.vo.AccessLevel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class StaffPermissionPersistenceAdapterTest {
    private final StaffPermissionJpaRepository repository = mock(StaffPermissionJpaRepository.class);
    private final StaffPermissionPersistenceAdapter adapter = new StaffPermissionPersistenceAdapter(repository);

    @Test
    void delegatesLookupDeleteAndSaveOperations() {
        StaffPermission entry = StaffPermission.of(10L, "STAFF_READ", AccessLevel.READ);
        when(repository.findByStaffId(10L)).thenReturn(List.of(entry));
        when(repository.saveAll(List.of(entry))).thenReturn(List.of(entry));

        assertThat(adapter.findByStaffId(10L)).containsExactly(entry);
        assertThat(adapter.saveAll(List.of(entry))).containsExactly(entry);
        adapter.deleteByStaffId(10L);

        verify(repository).findByStaffId(10L);
        verify(repository).deleteByStaffId(10L);
        verify(repository).saveAll(List.of(entry));
    }

    @Test
    void preservesEmptyResultsAndStorageFailures() {
        when(repository.findByStaffId(10L)).thenReturn(List.of());
        IllegalStateException failure = new IllegalStateException("database unavailable");
        doThrow(failure).when(repository).deleteByStaffId(11L);

        assertThat(adapter.findByStaffId(10L)).isEmpty();
        assertThatThrownBy(() -> adapter.deleteByStaffId(11L)).isSameAs(failure);
    }
}
