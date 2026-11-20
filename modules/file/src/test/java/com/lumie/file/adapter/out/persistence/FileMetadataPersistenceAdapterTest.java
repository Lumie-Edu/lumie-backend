package com.lumie.file.adapter.out.persistence;

import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.vo.EntityType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileMetadataPersistenceAdapterTest {
    private final FileMetadataJpaRepository repository = mock(FileMetadataJpaRepository.class);
    private final FileMetadataPersistenceAdapter adapter = new FileMetadataPersistenceAdapter(repository);

    @Test
    void delegatesEntityScopedQueriesAndEmptyResults() {
        when(repository.findByEntityTypeAndEntityId(EntityType.ANNOUNCEMENT, 41L)).thenReturn(List.of());

        assertThat(adapter.findByEntityTypeAndEntityId(EntityType.ANNOUNCEMENT, 41L)).isEmpty();
        verify(repository).findByEntityTypeAndEntityId(EntityType.ANNOUNCEMENT, 41L);
    }

    @Test
    void delegatesCrudOperationsWithoutChangingAbsenceOrEntityIdentity() {
        UUID id = UUID.randomUUID();
        FileMetadata metadata = mock(FileMetadata.class);
        when(repository.findById(id)).thenReturn(Optional.empty());
        when(repository.save(metadata)).thenReturn(metadata);

        assertThat(adapter.findById(id)).isEmpty();
        assertThat(adapter.save(metadata)).isSameAs(metadata);
        adapter.flush();
        adapter.delete(metadata);
        verify(repository).findById(id);
        verify(repository).save(metadata);
        verify(repository).flush();
        verify(repository).delete(metadata);
    }

    @Test
    void preservesStoreFailureInsteadOfConvertingItToAbsence() {
        UUID id = UUID.randomUUID();
        IllegalStateException failure = new IllegalStateException("store unavailable");
        when(repository.findById(id)).thenThrow(failure);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> adapter.findById(id)).isSameAs(failure);
    }

}
