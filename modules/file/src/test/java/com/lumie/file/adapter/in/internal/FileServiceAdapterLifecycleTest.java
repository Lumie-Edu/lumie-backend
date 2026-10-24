package com.lumie.file.adapter.in.internal;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.file.api.FileObjectKeyFactory;
import com.lumie.file.api.PreparedTemporaryFile;
import com.lumie.file.api.TemporaryAttachment;
import com.lumie.file.application.port.out.FileStorageOperationPersistencePort;
import com.lumie.file.application.port.out.StoragePort;
import com.lumie.file.application.service.TemporaryObjectUploadService;
import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.repository.FileMetadataRepository;
import com.lumie.file.domain.vo.EntityType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceAdapterLifecycleTest {

    private static final UUID UPLOAD_ID = UUID.fromString("10e28afa-87c1-47ee-847d-8940d85058ff");
    private static final String SOURCE = FileObjectKeyFactory.temporaryUpload("demo", UPLOAD_ID, "notes.pdf");

    @Mock private FileMetadataRepository metadataRepository;
    @Mock private StoragePort storage;
    @Mock private FileStorageOperationPersistencePort operationPersistencePort;

    private FileServiceAdapter adapter;

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenant("inst-demo");
        TenantContextHolder.setTenantId(42L);
        adapter = new FileServiceAdapter(metadataRepository, storage, operationPersistencePort);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void emptyPreparationAndNullDeleteRequestsAreNoOps() {
        assertThat(adapter.prepareTemporaryFiles("announcement", null)).isEmpty();
        assertThat(adapter.prepareTemporaryFiles("announcement", List.of())).isEmpty();
        adapter.attachPreparedFiles("announcement", 9L, null);
        adapter.attachPreparedFiles("announcement", 9L, List.of());
        adapter.deleteFilesByIds(null);
        adapter.deleteFilesByIds(List.of());
        adapter.deleteFilesByEntityAndIds("announcement", 9L, null);
        adapter.deleteFilesByEntityAndIds("announcement", 9L, List.of());
        adapter.discardPreparedFiles(null);
        adapter.reconcilePreparedDirectFiles("announcement", 9L, null, List.of());

        verifyNoInteractions(metadataRepository, storage, operationPersistencePort);
    }

    @Test
    void deleteByEntityWithNoFilesAndDeletingFilesByIdAreNoOps() {
        when(metadataRepository.findByEntityTypeAndEntityId(EntityType.ANNOUNCEMENT, 9L)).thenReturn(List.of());
        FileMetadata deleting = file(UUID.randomUUID(), EntityType.ANNOUNCEMENT, 9L, null,
                "files/demo/already-deleting.pdf");
        deleting.markDeleting();
        when(metadataRepository.findById(deleting.getId())).thenReturn(java.util.Optional.of(deleting));

        adapter.deleteFilesByEntity("announcement", 9L);
        adapter.deleteFilesByIds(List.of(deleting.getId()));

        verifyNoInteractions(storage, operationPersistencePort);
    }

    @Test
    void textbookFilesCannotBeDeletedThroughPostAttachmentIds() {
        FileMetadata textbook = file(UUID.randomUUID(), EntityType.TEXTBOOK, null, null, "files/demo/book.pdf");
        when(metadataRepository.findById(textbook.getId())).thenReturn(java.util.Optional.of(textbook));

        assertThatThrownBy(() -> adapter.deleteFilesByIds(List.of(textbook.getId())))
                .isInstanceOf(BusinessException.class);

        verifyNoInteractions(operationPersistencePort, storage);
    }

    @Test
    void rejectsTooManyAttachmentsAndNullOrDuplicateDescriptorsBeforeStorage() {
        List<TemporaryAttachment> tooMany = new ArrayList<>();
        for (int i = 0; i < 6; i++) tooMany.add(attachment("upload-" + i, "notes.pdf", 10L));
        assertThatThrownBy(() -> adapter.prepareTemporaryFiles("announcement", tooMany))
                .isInstanceOf(BusinessException.class);

        assertThatThrownBy(() -> adapter.prepareTemporaryFiles("announcement",
                java.util.Arrays.asList(attachment("upload-1", "notes.pdf", 10L), null)))
                .isInstanceOf(BusinessException.class);

        TemporaryAttachment duplicate = attachment("upload-1", "notes.pdf", 10L);
        assertThatThrownBy(() -> adapter.prepareTemporaryFiles("announcement", List.of(duplicate, duplicate)))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void rejectsInvalidIndividualMetadataBeforeStorage() {
        assertThatThrownBy(() -> adapter.prepareTemporaryFiles("announcement",
                List.of(attachment("upload-1", "notes.pdf", 0L)))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> adapter.prepareTemporaryFiles("announcement",
                List.of(attachment("upload-1", "notes.pdf", TemporaryObjectUploadService.MAX_BYTES + 1))))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> adapter.prepareTemporaryFiles("announcement",
                List.of(new TemporaryAttachment(SOURCE, "notes.pdf", " ", 10L))))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> adapter.prepareTemporaryFiles("announcement",
                List.of(new TemporaryAttachment(SOURCE, null, "application/pdf", 10L))))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> adapter.prepareTemporaryFiles("announcement",
                List.of(attachment("upload-1", "notes.exe", 10L))))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void rejectsUnknownEntityTypeBeforeStorage() {
        assertThatThrownBy(() -> adapter.prepareTemporaryFiles("unknown", List.of(attachment("upload-1", "notes.pdf", 10L))))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void temporarySourceMustExistAndMatchDeclaredSizeAndEtag() {
        when(storage.objectExists(SOURCE)).thenReturn(false);
        assertThatThrownBy(() -> adapter.prepareTemporaryFiles("announcement",
                List.of(new TemporaryAttachment(SOURCE, "notes.pdf", "application/pdf", 10L))))
                .isInstanceOf(BusinessException.class);

        when(storage.objectExists(SOURCE)).thenReturn(true);
        when(storage.objectSize(SOURCE)).thenReturn(9L);
        when(storage.objectContentType(SOURCE)).thenReturn("application/pdf");
        when(storage.objectEtag(SOURCE)).thenReturn("etag");
        when(storage.objectLastModified(SOURCE)).thenReturn(Instant.now());
        assertThatThrownBy(() -> adapter.prepareTemporaryFiles("announcement",
                List.of(new TemporaryAttachment(SOURCE, "notes.pdf", "application/pdf", 10L))))
                .isInstanceOf(BusinessException.class);

        when(storage.objectSize(SOURCE)).thenReturn(10L);
        when(storage.objectEtag(SOURCE)).thenReturn(" ");
        assertThatThrownBy(() -> adapter.prepareTemporaryFiles("announcement",
                List.of(new TemporaryAttachment(SOURCE, "notes.pdf", "application/pdf", 10L))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void discardingPreparedFilesContinuesWhenStorageCleanupFails() {
        PreparedTemporaryFile prepared = new PreparedTemporaryFile(
                UPLOAD_ID, "notes.pdf", "application/pdf", 10L,
                "tmp/demo/uploads/upload-1/notes.pdf", "files/demo/notes.pdf", null, null);
        org.mockito.BDDMockito.willThrow(new RuntimeException("storage down"))
                .given(storage).deleteObject("files/demo/notes.pdf");

        adapter.discardPreparedFiles(List.of(prepared));

        verify(storage).deleteObject("files/demo/notes.pdf");
    }

    private static TemporaryAttachment attachment(String uploadId, String filename, long size) {
        UUID id = UUID.nameUUIDFromBytes(uploadId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String objectKey = FileObjectKeyFactory.temporaryUpload("demo", id, filename);
        return new TemporaryAttachment(objectKey, filename, "application/pdf", size);
    }

    private static FileMetadata file(UUID id, EntityType type, Long entityId, UUID folderId, String key) {
        return FileMetadata.createWithId(id, type, entityId, "notes.pdf", "application/pdf", 10L, key, folderId);
    }
}
