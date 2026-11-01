package com.lumie.file.adapter.out.persistence;

import com.lumie.file.domain.entity.FileUploadSession;
import jakarta.persistence.LockModeType;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileUploadSessionPersistenceAdapterTest {
    private final FileUploadSessionJpaRepository repository = mock(FileUploadSessionJpaRepository.class);
    private final FileUploadSessionPersistenceAdapter adapter = new FileUploadSessionPersistenceAdapter(repository);

    @Test
    void preservesOptionalAbsenceAndForUpdateLookup() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.empty());

        assertThat(adapter.findById(id)).isEmpty();
        assertThat(adapter.findByIdForUpdate(id)).isEmpty();
        verify(repository).findById(id);
        verify(repository).findByIdForUpdate(id);
    }

    @Test
    void preservesPessimisticWriteLockForStateTransitions() throws NoSuchMethodException {
        Method method = FileUploadSessionJpaRepository.class.getMethod("findByIdForUpdate", UUID.class);

        assertThat(method.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void delegatesSessionSave() {
        FileUploadSession session = mock(FileUploadSession.class);
        when(repository.save(session)).thenReturn(session);

        assertThat(adapter.save(session)).isSameAs(session);
        verify(repository).save(session);
    }

    @Test
    void propagatesStoreFailure() {
        UUID id = UUID.randomUUID();
        RuntimeException failure = new IllegalStateException("database unavailable");
        when(repository.findByIdForUpdate(id)).thenThrow(failure);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> adapter.findByIdForUpdate(id)).isSameAs(failure);
    }
}
