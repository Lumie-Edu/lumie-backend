package com.lumie.file.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileDownloadPersistenceAdapterTest {
    private final FileDownloadJpaRepository repository = mock(FileDownloadJpaRepository.class);
    private final FileDownloadPersistenceAdapter adapter = new FileDownloadPersistenceAdapter(repository);

    @Test
    void upsertDownloadDelegatesTenantAndIdentityAndReturnsMutationCount() {
        UUID fileId = UUID.randomUUID();
        when(repository.upsertDownload(7L, fileId, 11L)).thenReturn(1);

        assertThat(adapter.upsertDownload(7L, fileId, 11L)).isEqualTo(1);
        verify(repository).upsertDownload(7L, fileId, 11L);
    }

    @Test
    void findByFileIdPreservesRepositoryOrderingAndEmptyResult() {
        UUID fileId = UUID.randomUUID();
        when(repository.findByFileIdOrderByUpdatedAtDesc(fileId)).thenReturn(List.of());

        assertThat(adapter.findByFileIdOrderByUpdatedAtDesc(fileId)).isEmpty();
        verify(repository).findByFileIdOrderByUpdatedAtDesc(fileId);
    }

    @Test
    void upsertRemainsOneAtomicConflictUpdate() throws NoSuchMethodException {
        var method = FileDownloadJpaRepository.class.getMethod("upsertDownload", Long.class, UUID.class, Long.class);

        assertThat(method.getAnnotation(Modifying.class)).isNotNull();
        assertThat(method.getAnnotation(Query.class).value())
                .contains("on conflict (tenant_id, file_id, user_id)")
                .contains("download_count = file_download.download_count + 1");
    }

    @Test
    void propagatesStoreFailure() {
        UUID fileId = UUID.randomUUID();
        RuntimeException failure = new IllegalStateException("database unavailable");
        org.mockito.Mockito.when(repository.findByFileIdOrderByUpdatedAtDesc(fileId)).thenThrow(failure);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> adapter.findByFileIdOrderByUpdatedAtDesc(fileId))
                .isSameAs(failure);
    }
}
