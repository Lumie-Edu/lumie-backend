package com.lumie.file.adapter.in.internal;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.file.api.FileObjectKeyFactory;
import com.lumie.file.api.PreparedTemporaryFile;
import com.lumie.file.api.TemporaryAttachment;
import com.lumie.file.application.port.out.FileStorageOperationPersistencePort;
import com.lumie.file.application.port.out.StoragePort;
import com.lumie.file.domain.repository.FileMetadataRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class FileServiceAdapterSourceValidationTest {

    private static final UUID UPLOAD_ID = UUID.fromString("08258b08-3507-4f81-8c2a-4a0dc6da5c8e");
    private static final String SOURCE = FileObjectKeyFactory.temporaryUpload("demo", UPLOAD_ID, "notes.pdf");
    private final StoragePort storage = mock(StoragePort.class);
    private final FileServiceAdapter adapter = new FileServiceAdapter(
            mock(FileMetadataRepository.class), storage, mock(FileStorageOperationPersistencePort.class));

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void rejectsTemporaryObjectOlderThanTheTwoHourSafetyMargin() {
        prepareTenant();
        givenValidSource();
        when(storage.objectLastModified(SOURCE)).thenReturn(Instant.now().minusSeconds(22 * 60 * 60 + 1));

        assertThatThrownBy(() -> adapter.prepareTemporaryFiles("announcement", List.of(attachment())))
                .isInstanceOf(BusinessException.class);
        verify(storage, never()).copyObject(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void rejectsSourceContentTypeThatDiffersFromDeclaredMetadata() {
        prepareTenant();
        givenValidSource();
        when(storage.objectContentType(SOURCE)).thenReturn("application/octet-stream");

        assertThatThrownBy(() -> adapter.prepareTemporaryFiles("announcement", List.of(attachment())))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void capturesObservedSourceIdentityAndTimestampForPromotionIntent() {
        prepareTenant();
        givenValidSource();
        Instant modified = Instant.parse("2026-10-20T00:00:00Z");
        when(storage.objectLastModified(SOURCE)).thenReturn(modified);

        List<PreparedTemporaryFile> prepared = adapter.prepareTemporaryFiles("announcement", List.of(attachment()));

        assertThat(prepared).singleElement().satisfies(file -> {
            assertThat(file.temporaryObjectKey()).isEqualTo(SOURCE);
            assertThat(file.sourceEtag()).isEqualTo("etag-1");
            assertThat(file.sourceLastModified()).isEqualTo(modified);
        });
        verify(storage, never()).copyObject(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    private void givenValidSource() {
        when(storage.objectExists(SOURCE)).thenReturn(true);
        when(storage.objectSize(SOURCE)).thenReturn(10L);
        when(storage.objectContentType(SOURCE)).thenReturn("application/pdf");
        when(storage.objectEtag(SOURCE)).thenReturn("etag-1");
        when(storage.objectLastModified(SOURCE)).thenReturn(Instant.now());
    }

    private static void prepareTenant() {
        TenantContextHolder.setTenant("inst-demo");
        TenantContextHolder.setTenantId(42L);
    }

    private static TemporaryAttachment attachment() {
        return new TemporaryAttachment(SOURCE, "notes.pdf", "application/pdf", 10L);
    }
}
