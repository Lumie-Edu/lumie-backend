package com.lumie.file.adapter.in.internal;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.file.api.FileObjectKeyFactory;
import com.lumie.file.api.TemporaryAttachment;
import com.lumie.file.application.port.out.StoragePort;
import com.lumie.file.domain.repository.FileMetadataRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class FileServiceAdapterTemporaryKeyTest {
    private static final UUID FILE_ID = UUID.fromString("10e28afa-87c1-47ee-847d-8940d85058ff");
    private final StoragePort storage = mock(StoragePort.class);
    private final FileServiceAdapter adapter = new FileServiceAdapter(mock(FileMetadataRepository.class), storage);

    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
    }

    @Test
    void rejectsAnExistingTenantKeyWhenItsFilenameDoesNotMatchTheAttachment() {
        TenantContextHolder.setTenant("inst-demo");
        String key = FileObjectKeyFactory.temporaryUpload("demo", FILE_ID, "other.pdf");

        assertThatThrownBy(() -> adapter.prepareTemporaryFiles("announcement",
                List.of(new TemporaryAttachment(key, "requested.pdf", "application/pdf", 10))))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void canonicalKeyReachesStorageValidationAndInvalidUuidIsRejected() {
        TenantContextHolder.setTenant("inst-demo");
        String key = FileObjectKeyFactory.temporaryUpload("demo", FILE_ID, "requested.pdf");
        assertThat(FileObjectKeyFactory.isTemporaryUploadKey("demo", key, "requested.pdf")).isTrue();
        assertThat(FileObjectKeyFactory.isTemporaryUploadKey("demo",
                "tmp/demo/uploads/not-a-uuid/requested.pdf", "requested.pdf")).isFalse();

        assertThatThrownBy(() -> adapter.prepareTemporaryFiles("announcement",
                List.of(new TemporaryAttachment(key, "requested.pdf", "application/pdf", 10))))
                .isInstanceOf(BusinessException.class);
        verify(storage).objectExists(key);
    }
}
