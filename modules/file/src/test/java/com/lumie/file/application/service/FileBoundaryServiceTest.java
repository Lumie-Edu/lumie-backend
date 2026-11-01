package com.lumie.file.application.service;

import java.lang.reflect.Method;
import com.lumie.file.adapter.out.persistence.FileUploadSessionJpaRepository;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Lock;

import static org.assertj.core.api.Assertions.assertThat;

class FileBoundaryServiceTest {
    @Test
    void downloadAndStorageProcessingRunOutsideAnOpenDatabaseTransaction() throws NoSuchMethodException {
        Method download = FileQueryService.class.getMethod("downloadFile", java.util.UUID.class);
        Method process = StorageOperationProcessor.class.getMethod("processDueOperations", int.class, String.class);

        assertThat(download.getAnnotation(Transactional.class)).isNull();
        assertThat(process.getAnnotation(Transactional.class)).isNull();
        assertThat(FileQueryService.class.getAnnotation(Transactional.class)).isNull();
        assertThat(StorageOperationProcessor.class.getAnnotation(Transactional.class)).isNull();
    }

    @Test
    void uploadSessionStateChangesRemainTransactionalAndUseTheLockedPortOperation() throws NoSuchMethodException {
        Method markUploaded = UploadSessionStateService.class.getMethod("markUploaded", java.util.UUID.class, String.class);
        Method markUploadedLookup = FileUploadSessionJpaRepository.class
                .getMethod("findByIdForUpdate", java.util.UUID.class);

        assertThat(markUploaded.getAnnotation(Transactional.class)).isNotNull();
        assertThat(markUploadedLookup.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }
}
