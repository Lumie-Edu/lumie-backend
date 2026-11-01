package com.lumie.file.adapter.out.persistence;

import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.vo.EntityType;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileMetadataPersistenceAdapterTest {
    private final FileMetadataJpaRepository repository = mock(FileMetadataJpaRepository.class);
    private final FileMetadataPersistenceAdapter adapter = new FileMetadataPersistenceAdapter(repository);

    @Test
    void delegatesEntityScopedQueriesIncludingNullFolderAndEmptyResults() {
        UUID folderId = UUID.randomUUID();
        when(repository.findByEntityTypeAndEntityId(EntityType.ANNOUNCEMENT, 41L)).thenReturn(List.of());
        when(repository.findByEntityTypeAndFolderId(EntityType.TEXTBOOK, null)).thenReturn(List.of());
        when(repository.findByEntityType(EntityType.TEXTBOOK)).thenReturn(List.of());
        when(repository.existsByFolderId(folderId)).thenReturn(false);

        assertThat(adapter.findByEntityTypeAndEntityId(EntityType.ANNOUNCEMENT, 41L)).isEmpty();
        assertThat(adapter.findByEntityTypeAndFolderId(EntityType.TEXTBOOK, null)).isEmpty();
        assertThat(adapter.findByEntityType(EntityType.TEXTBOOK)).isEmpty();
        assertThat(adapter.existsByFolderId(folderId)).isFalse();
        verify(repository).findByEntityTypeAndEntityId(EntityType.ANNOUNCEMENT, 41L);
        verify(repository).findByEntityTypeAndFolderId(EntityType.TEXTBOOK, null);
        verify(repository).findByEntityType(EntityType.TEXTBOOK);
        verify(repository).existsByFolderId(folderId);
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

    @Test
    void repositoryQueriesKeepNullAwareFolderFilterAndStableOrdering() throws NoSuchMethodException {
        Method byFolder = FileMetadataJpaRepository.class
                .getMethod("findByEntityTypeAndFolderId", EntityType.class, UUID.class);
        Method byType = FileMetadataJpaRepository.class.getMethod("findByEntityType", EntityType.class);

        assertThat(byFolder.getAnnotation(Query.class).value())
                .contains(":folderId IS NULL AND f.folderId IS NULL")
                .contains("ORDER BY f.createdAt DESC, f.originalFilename ASC, f.id DESC");
        assertThat(byType.getAnnotation(Query.class).value())
                .contains("ORDER BY f.createdAt DESC, f.originalFilename ASC, f.id DESC");
    }
}
