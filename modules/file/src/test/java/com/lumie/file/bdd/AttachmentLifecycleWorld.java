package com.lumie.file.bdd;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.auth.Role;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.content.api.ContentService;
import com.lumie.file.adapter.in.internal.FileServiceAdapter;
import com.lumie.file.application.port.out.FileStorageOperationPersistencePort;
import com.lumie.file.application.port.out.FileStorageOperationMetricsPersistencePort;
import com.lumie.file.application.port.out.StoragePort;
import com.lumie.file.application.service.FileMetadataLookupService;
import com.lumie.file.application.service.FileCommandService;
import com.lumie.file.application.service.FileQueryService;
import com.lumie.file.application.service.StorageOperationProcessor;
import com.lumie.file.application.service.StorageOperationMetrics;
import com.lumie.file.application.service.TextbookFolderCommandService;
import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.entity.FileStorageOperation;
import com.lumie.file.domain.entity.TextbookFolder;
import com.lumie.file.domain.repository.FileDownloadRepository;
import com.lumie.file.domain.repository.FileMetadataRepository;
import com.lumie.file.domain.repository.TextbookFolderRepository;
import com.lumie.file.domain.vo.EntityType;
import com.lumie.file.domain.vo.FileUploadStatus;
import com.lumie.student.api.StudentService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.io.ByteArrayInputStream;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

/** Scenario-owned test fixtures shared by the attachment lifecycle step classes. */
public class AttachmentLifecycleWorld {

    public final UUID fileId = UUID.fromString("c264c122-a715-47a7-8e80-2d7ddac8476a");
    public final List<FileMetadata> files = new ArrayList<>();
    public final FileMetadataRepository fileMetadataRepository = mock(FileMetadataRepository.class);
    public final TextbookFolderRepository textbookFolderRepository = mock(TextbookFolderRepository.class);
    public final StoragePort storagePort = mock(StoragePort.class);
    public final FileStorageOperationPersistencePort operationPersistencePort =
            mock(FileStorageOperationPersistencePort.class);
    public final FileStorageOperationMetricsPersistencePort metricsPersistencePort =
            mock(FileStorageOperationMetricsPersistencePort.class);
    public final SimpleMeterRegistry metricsRegistry = new SimpleMeterRegistry();
    public final StorageOperationMetrics metrics = new StorageOperationMetrics(metricsRegistry, metricsPersistencePort);
    public final FileServiceAdapter fileServiceAdapter =
            new FileServiceAdapter(fileMetadataRepository, storagePort, operationPersistencePort);
    public final FileCommandService fileCommandService = new FileCommandService(
            fileMetadataRepository, textbookFolderRepository,
            mock(com.lumie.file.domain.repository.FileDownloadRepository.class), mock(com.lumie.file.api.FileService.class),
            operationPersistencePort);
    public final FileQueryService fileQueryService;
    public final StorageOperationProcessor storageOperationProcessor;
    public final TextbookFolderCommandService textbookFolderCommandService =
            new TextbookFolderCommandService(textbookFolderRepository, fileMetadataRepository, operationPersistencePort);
    public final AtomicReference<FileStorageOperation> operation = new AtomicReference<>();
    public final List<FileStorageOperation> storageOperations = new ArrayList<>();
    public com.lumie.file.api.PreparedTemporaryFile preparedTemporaryFile;
    public List<UUID> registeredIds = List.of();
    public UUID requestedRetainedId;

    public void preparePromotionCompletedBeforeDelete() {
        registerReadyAttachment(EntityType.ANNOUNCEMENT, 9L, null);
        FileStorageOperation promotion = FileStorageOperation.promote(
                fileId, "tmp/demo/uploads/upload-1/notes.pdf", files.getFirst().getObjectKey(),
                "etag-1", 10L, "application/pdf", Instant.now().minusSeconds(60));
        ReflectionTestUtils.setField(promotion, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(promotion, "tenantId", TenantContextHolder.getRequiredTenantId());
        promotion.markCompleted(Instant.now());
        operation.set(promotion);
        storageOperations.add(promotion);
        files.getFirst().markDeleting();
        recordOperation(FileStorageOperation.delete(fileId, files.getFirst().getObjectKey(), Instant.now()));
    }

    public void prepareTenantPromotion() {
        preparedTemporaryFile = new com.lumie.file.api.PreparedTemporaryFile(
                fileId, "notes.pdf", "application/pdf", 10L,
                "tmp/demo/uploads/upload-1/notes.pdf", "files/demo/announcement/file-1/notes.pdf",
                "source-etag", Instant.now().minusSeconds(60));
        registerPreparedAttachment();
    }

    public void prepareTerminalDeleteFailure() {
        FileMetadata metadata = FileMetadata.createWithId(fileId, EntityType.ANNOUNCEMENT, 9L,
                "notes.pdf", "application/pdf", 10L, "files/demo/announcement/file-1/notes.pdf", null);
        ReflectionTestUtils.setField(metadata, "tenantId", TenantContextHolder.getRequiredTenantId());
        metadata.markDeleting();
        files.add(metadata);
        FileStorageOperation delete = FileStorageOperation.delete(
                fileId, metadata.getObjectKey(), Instant.now().minus(java.time.Duration.ofDays(8)));
        ReflectionTestUtils.setField(delete, "deadlineAt", Instant.now().minusSeconds(1));
        recordOperation(delete);
    }

    public void prepareStaleMetricsSnapshot() {
        Instant now = Instant.now();
        FileStorageOperation retry = FileStorageOperation.delete(
                fileId, "files/demo/announcement/retry.pdf", now.minusSeconds(600));
        retry.claim("worker-a", now.minusSeconds(300), java.time.Duration.ofMinutes(2));
        retry.scheduleRetry(now.minusSeconds(300), now.minusSeconds(270), "STORAGE_ERROR", "transient failure");
        ReflectionTestUtils.setField(retry, "tenantId", TenantContextHolder.getRequiredTenantId());
        ReflectionTestUtils.setField(retry, "id", UUID.randomUUID());
        operation.set(retry);
        when(metricsPersistencePort.snapshot(any())).thenReturn(
                new FileStorageOperationMetricsPersistencePort.StorageOperationMetricsSnapshot(
                        List.of(new FileStorageOperationMetricsPersistencePort.Series("delete", "retry", 1L)),
                        List.of(new FileStorageOperationMetricsPersistencePort.Series("delete", "retry", 301L)), 0L));
        metrics.beginSnapshotRefresh();
        metrics.refreshCurrentTenantSnapshot();
    }

    private void recordOperation(FileStorageOperation value) {
        if (value.getId() == null) ReflectionTestUtils.setField(value, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(value, "tenantId", TenantContextHolder.getRequiredTenantId());
        operation.set(value);
        if (!storageOperations.contains(value)) storageOperations.add(value);
    }

    public void registerPreparedAttachment() {
        registeredIds = fileServiceAdapter.attachPreparedFiles(
                "announcement", 9L, List.of(preparedTemporaryFile));
    }

    public void configureInvalidPromotion(String condition) {
        String source = preparedTemporaryFile.temporaryObjectKey();
        String target = preparedTemporaryFile.durableObjectKey();
        if ("missing".equals(condition)) {
            when(storagePort.objectExists(source)).thenReturn(false);
        } else {
            when(storagePort.objectExists(source)).thenReturn(true);
        }
        when(storagePort.objectExists(target)).thenReturn(false, true);
        if ("size-mismatched".equals(condition)) {
            when(storagePort.objectSize(target)).thenReturn(preparedTemporaryFile.fileSize() + 1);
        } else {
            when(storagePort.objectSize(target)).thenReturn(preparedTemporaryFile.fileSize());
        }
        if ("metadata-mismatched".equals(condition)) {
            when(storagePort.objectContentType(target)).thenReturn("text/plain");
        } else {
            when(storagePort.objectContentType(target)).thenReturn(preparedTemporaryFile.contentType());
        }
    }

    public void registerReadyAttachment() {
        registerReadyAttachment(EntityType.ANNOUNCEMENT, 9L, null);
    }

    public void registerReadyAttachment(EntityType entityType, Long entityId, UUID folderId) {
        String key = "files/demo/" + entityType.getPathSegment() + "/file-1/notes.pdf";
        FileMetadata metadata = FileMetadata.createWithId(fileId, entityType, entityId,
                "notes.pdf", "application/pdf", 10L, key, folderId);
        ReflectionTestUtils.setField(metadata, "tenantId", 42L);
        metadata.markReady();
        files.add(metadata);
    }

    public void prepareTextbookFolder(UUID folderId) {
        when(textbookFolderRepository.findById(folderId)).thenReturn(java.util.Optional.of(mock(TextbookFolder.class)));
        when(textbookFolderRepository.findByParentId(folderId)).thenReturn(List.of());
    }

    public void preparePartialFolderDeleteFailure() {
        UUID folderId = UUID.fromString("8f72cc89-7c4e-4f7e-9bd0-99ffdfccbb09");
        UUID secondFileId = UUID.fromString("cc13408d-e89d-4321-813f-0febfa75ac14");
        String firstKey = "files/demo/textbook/first.pdf";
        String secondKey = "files/demo/textbook/second.pdf";
        registerReadyAttachment(EntityType.TEXTBOOK, null, folderId);
        FileMetadata second = FileMetadata.createWithId(secondFileId, EntityType.TEXTBOOK, null,
                "second.pdf", "application/pdf", 10L, secondKey, folderId);
        second.markReady();
        ReflectionTestUtils.setField(second, "tenantId", 42L);
        files.add(second);
        ReflectionTestUtils.setField(files.getFirst(), "objectKey", firstKey);
        prepareTextbookFolder(folderId);
        when(storagePort.objectExists(firstKey)).thenReturn(true, false);
        when(storagePort.objectExists(secondKey)).thenReturn(true);
        org.mockito.Mockito.doAnswer(invocation -> {
            if (secondKey.equals(invocation.getArgument(0))) {
                throw new com.lumie.common.exception.BusinessException(
                        com.lumie.file.domain.exception.FileErrorCode.STORAGE_ERROR);
            }
            return null;
        }).when(storagePort).deleteObject(any());
    }

    public void prepareAlreadyAbsentDelete() {
        String target = "files/demo/announcement/absent.pdf";
        FileStorageOperation delete = FileStorageOperation.delete(fileId, target, Instant.now());
        ReflectionTestUtils.setField(delete, "id", UUID.fromString("94398fc1-002a-4ea8-9ce9-f68920490251"));
        ReflectionTestUtils.setField(delete, "tenantId", TenantContextHolder.getRequiredTenantId());
        operation.set(delete);
        storageOperations.add(delete);
        when(storagePort.objectExists(target)).thenReturn(false);
    }

    public void prepareInvalidRetainedId(String ownership) {
        FileMetadata current = FileMetadata.createWithId(fileId, EntityType.ANNOUNCEMENT, 9L,
                "current.pdf", "application/pdf", 10L, "files/demo/announcement/current.pdf", null);
        ReflectionTestUtils.setField(current, "tenantId", 42L);
        current.markReady();
        files.add(current);
        requestedRetainedId = UUID.randomUUID();
        if ("owned by another announcement".equals(ownership)) {
            FileMetadata foreign = FileMetadata.createWithId(requestedRetainedId, EntityType.ANNOUNCEMENT, 10L,
                    "foreign.pdf", "application/pdf", 10L, "files/demo/announcement/foreign.pdf", null);
            foreign.markReady();
            ReflectionTestUtils.setField(foreign, "tenantId", 42L);
            files.add(foreign);
        } else if ("owned by another tenant".equals(ownership)) {
            FileMetadata foreign = FileMetadata.createWithId(requestedRetainedId, EntityType.ANNOUNCEMENT, 9L,
                    "foreign.pdf", "application/pdf", 10L, "files/other/announcement/foreign.pdf", null);
            foreign.markReady();
            ReflectionTestUtils.setField(foreign, "tenantId", 99L);
            files.add(foreign);
        }
    }

    public AttachmentLifecycleWorld() {
        TenantContextHolder.setTenant("inst-demo");
        TenantContextHolder.setTenantId(42L);
        UserContextHolder.setUserRole(Role.INSTRUCTOR);
        UserContextHolder.setUserId(7L);
        when(fileMetadataRepository.save(any())).thenAnswer(invocation -> {
            FileMetadata metadata = invocation.getArgument(0);
            ReflectionTestUtils.setField(metadata, "id", fileId);
            ReflectionTestUtils.setField(metadata, "tenantId", TenantContextHolder.getRequiredTenantId());
            files.add(metadata);
            return metadata;
        });
        doAnswer(invocation -> {
            FileStorageOperation value = invocation.getArgument(0);
            recordOperation(value);
            return value;
        }).when(operationPersistencePort).save(any());
        when(operationPersistencePort.claimDueOperations(any(), anyInt(), any(), any()))
                .thenAnswer(invocation -> storageOperations.stream()
                        .filter(item -> java.util.Objects.equals(item.getTenantId(),
                                TenantContextHolder.getRequiredTenantId()))
                        .filter(item -> item.getStatus() != com.lumie.file.domain.vo.FileStorageOperationStatus.COMPLETED
                                && item.getStatus() != com.lumie.file.domain.vo.FileStorageOperationStatus.FAILED)
                        .toList());
        doAnswer(invocation -> {
            FileStorageOperation completed = storageOperations.stream()
                    .filter(item -> item.getId().equals(invocation.getArgument(0)))
                    .findFirst().orElseThrow();
            FileMetadata metadata = files.stream()
                    .filter(file -> fileId.equals(file.getId()))
                    .findFirst().orElse(null);
            if (metadata != null && metadata.getUploadStatus() == FileUploadStatus.PENDING) {
                metadata.markReady();
            } else if (metadata == null || !metadata.isReady()) {
                String target = metadata == null ? completed.getTargetObjectKey() : metadata.getObjectKey();
                FileStorageOperation followup = FileStorageOperation.delete(
                        completed.getFileId(), target, Instant.now());
                recordOperation(followup);
            }
            completed.markCompleted(Instant.now());
            return null;
        }).when(operationPersistencePort).finalizePromotion(any(), any(), any());
        doAnswer(invocation -> {
            FileMetadata metadata = files.stream()
                    .filter(file -> fileId.equals(file.getId()))
                    .findFirst().orElse(null);
            if (metadata != null && metadata.getUploadStatus() == FileUploadStatus.PENDING) metadata.markFailed();
            return null;
        }).when(operationPersistencePort).finalizeFailure(any(), any(), any(), any(), any());
        doAnswer(invocation -> {
            UUID deletedFileId = invocation.getArgument(1);
            FileStorageOperation completed = storageOperations.stream()
                    .filter(item -> item.getId().equals(invocation.getArgument(0)))
                    .findFirst().orElseThrow();
            completed.markCompleted(Instant.now());
            files.removeIf(file -> deletedFileId.equals(file.getId()));
            return null;
        }).when(operationPersistencePort).finalizeDeletion(any(), any(), any());
        when(fileMetadataRepository.findById(any())).thenAnswer(invocation -> files.stream()
                .filter(file -> fileId.equals(invocation.getArgument(0)))
                .filter(file -> java.util.Objects.equals(file.getTenantId(), TenantContextHolder.getRequiredTenantId()))
                .findFirst());
        when(fileMetadataRepository.findByEntityTypeAndEntityId(any(), anyLong()))
                .thenAnswer(invocation -> files.stream()
                        .filter(file -> file.getEntityType() == invocation.getArgument(0))
                        .filter(file -> java.util.Objects.equals(file.getTenantId(), TenantContextHolder.getRequiredTenantId()))
                        .filter(file -> java.util.Objects.equals(file.getEntityId(), invocation.getArgument(1)))
                        .toList());
        when(fileMetadataRepository.findByEntityTypeAndFolderId(any(), any()))
                .thenAnswer(invocation -> files.stream()
                        .filter(file -> file.getEntityType() == invocation.getArgument(0))
                        .filter(file -> java.util.Objects.equals(file.getTenantId(), TenantContextHolder.getRequiredTenantId()))
                        .filter(file -> java.util.Objects.equals(file.getFolderId(), invocation.getArgument(1)))
                        .toList());
        when(fileMetadataRepository.findByEntityType(any())).thenAnswer(invocation -> files.stream()
                .filter(file -> file.getEntityType() == invocation.getArgument(0))
                .filter(file -> java.util.Objects.equals(file.getTenantId(), TenantContextHolder.getRequiredTenantId()))
                .toList());

        FileMetadataLookupService lookup = new FileMetadataLookupService(
                fileMetadataRepository, mock(ContentService.class));
        fileQueryService = new FileQueryService(
                fileMetadataRepository,
                mock(FileDownloadRepository.class),
                mock(StudentService.class),
                lookup,
                storagePort);
        metrics.initializeMeters();
        storageOperationProcessor = new StorageOperationProcessor(
                operationPersistencePort, fileMetadataRepository, storagePort, java.time.Clock.systemUTC(), metrics);
        when(storagePort.objectExists("tmp/demo/uploads/upload-1/notes.pdf")).thenReturn(true);
        when(storagePort.objectExists("files/demo/announcement/file-1/notes.pdf")).thenReturn(false, true);
        when(storagePort.objectSize("files/demo/announcement/file-1/notes.pdf")).thenReturn(10L);
        when(storagePort.objectContentType("files/demo/announcement/file-1/notes.pdf"))
                .thenReturn("application/pdf");
        when(storagePort.getObject("files/demo/announcement/file-1/notes.pdf"))
                .thenReturn(new ByteArrayInputStream(new byte[10]));
    }
}
