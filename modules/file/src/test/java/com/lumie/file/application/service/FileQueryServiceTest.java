package com.lumie.file.application.service;

import com.lumie.common.auth.Role;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.file.application.port.out.StoragePort;
import com.lumie.file.application.dto.response.FileMetadataResponse;
import com.lumie.file.domain.entity.FileDownload;
import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.repository.FileDownloadRepository;
import com.lumie.file.domain.repository.FileMetadataRepository;
import com.lumie.file.domain.exception.FileErrorCode;
import com.lumie.file.domain.vo.EntityType;
import com.lumie.file.domain.vo.FileUploadStatus;
import com.lumie.student.api.StudentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileQueryService 테스트")
class FileQueryServiceTest {

    @Mock private FileMetadataRepository fileMetadataRepository;
    @Mock private FileDownloadRepository fileDownloadRepository;
    @Mock private StudentService studentService;
    @Mock private FileMetadataLookupService fileMetadataLookupService;
    @Mock private StoragePort storagePort;
    @InjectMocks private FileQueryService queryService;

    @BeforeEach
    void setUpContext() {
        TenantContextHolder.setTenant("test-tenant");
        UserContextHolder.setUserRole(Role.OWNER);
    }

    @AfterEach
    void tearDownContext() {
        TenantContextHolder.clear();
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("다운로드한 학생을 찾을 수 없으면 '삭제된 학생'으로 반환한다")
    void getFileDownloaders_deletedStudentName() {
        UUID fileId = UUID.randomUUID();
        FileMetadata metadata = FileMetadata.create(EntityType.ANNOUNCEMENT, 1L, "file.pdf", "application/pdf", 1L, "object", null);
        FileDownload download = FileDownload.create(fileId, 100L);
        given(fileMetadataRepository.findById(fileId)).willReturn(Optional.of(metadata));
        given(fileDownloadRepository.findByFileIdOrderByUpdatedAtDesc(fileId)).willReturn(List.of(download));
        given(studentService.getStudentsByUserIds("test-tenant", List.of(100L)))
                .willReturn(new StudentService.StudentListData(List.of(), 0, 0, 0));

        var response = queryService.getFileDownloaders(fileId);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).userName()).isEqualTo("삭제된 학생");
    }

    @Test
    void getFileDownloadersReturnsEmptyWithoutCallingStudentServiceWhenThereAreNoDownloads() {
        UUID fileId = UUID.randomUUID();
        given(fileMetadataRepository.findById(fileId)).willReturn(Optional.of(
                FileMetadata.create(EntityType.TEXTBOOK, null, "file.pdf", "application/pdf", 1L, "object", null)));
        given(fileDownloadRepository.findByFileIdOrderByUpdatedAtDesc(fileId)).willReturn(List.of());

        assertThat(queryService.getFileDownloaders(fileId)).isEmpty();

        verifyNoInteractions(studentService);
    }

    @Test
    void getFileDownloadersUsesResolvedStudentNameWhenAvailable() {
        UUID fileId = UUID.randomUUID();
        FileDownload download = FileDownload.create(fileId, 100L);
        given(fileMetadataRepository.findById(fileId)).willReturn(Optional.of(
                FileMetadata.create(EntityType.TEXTBOOK, null, "file.pdf", "application/pdf", 1L, "object", null)));
        given(fileDownloadRepository.findByFileIdOrderByUpdatedAtDesc(fileId)).willReturn(List.of(download));
        given(studentService.getStudentsByUserIds("test-tenant", List.of(100L)))
                .willReturn(new StudentService.StudentListData(
                        List.of(new StudentService.StudentData(1L, 100L, "학생 이름", null, true, null, null)), 1, 0, 1));

        assertThat(queryService.getFileDownloaders(fileId).getFirst().userName()).isEqualTo("학생 이름");
    }

    @Test
    void readyDetailAndDownloadUseTheAuthorizedMetadataAndStorageObject() {
        UUID fileId = UUID.randomUUID();
        FileMetadata metadata = FileMetadata.createWithId(fileId, EntityType.TEXTBOOK, null,
                "lesson.pdf", "application/pdf", 10L, "files/demo/lesson.pdf", null);
        metadata.markReady();
        given(fileMetadataRepository.findById(fileId)).willReturn(Optional.of(metadata));
        InputStreamFixture input = new InputStreamFixture();
        given(fileMetadataLookupService.getAuthorizedDownloadMetadata(fileId)).willReturn(
                new FileMetadataLookupService.DownloadMetadata(
                        fileId, metadata.getObjectKey(), metadata.getOriginalFilename(), metadata.getContentType(), metadata.getFileSize()));
        given(storagePort.getObject(metadata.getObjectKey())).willReturn(input.stream);

        assertThat(queryService.getFileMetadata(fileId).id()).isEqualTo(fileId);
        var download = queryService.downloadFile(fileId);
        assertThat(download.inputStream()).isSameAs(input.stream);
        assertThat(download.filename()).isEqualTo("lesson.pdf");
    }

    private static final class InputStreamFixture {
        private final java.io.InputStream stream = new ByteArrayInputStream(new byte[]{1});
    }

    @Test
    void metadataReadEnforcesTheSameEntityPolicyAsDownload() {
        UUID fileId = UUID.randomUUID();
        FileMetadata metadata = FileMetadata.create(EntityType.QNA, 8L, "private.pdf",
                "application/pdf", 1L, "object", null);
        given(fileMetadataRepository.findById(fileId)).willReturn(Optional.of(metadata));
        doThrow(new BusinessException(FileErrorCode.ACCESS_DENIED))
                .when(fileMetadataLookupService).requireAccess(EntityType.QNA, 8L);

        assertThatThrownBy(() -> queryService.getFileMetadata(fileId))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.ACCESS_DENIED));
        verify(fileMetadataLookupService).requireAccess(EntityType.QNA, 8L);
    }

    @Test
    void inaccessibleCollectionIsRejectedBeforeMetadataQuery() {
        doThrow(new BusinessException(FileErrorCode.ACCESS_DENIED))
                .when(fileMetadataLookupService).requireAccess(EntityType.QNA, 8L);

        assertThatThrownBy(() -> queryService.getFilesByEntity(EntityType.QNA, 8L))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(fileMetadataRepository);
    }

    @Test
    void staffTextbookCollectionUsesTheSharedAccessPolicy() {
        queryService.getAllFilesByEntityType(EntityType.TEXTBOOK);

        verify(fileMetadataLookupService).requireAccess(EntityType.TEXTBOOK, null);
        verify(fileMetadataRepository).findByEntityType(EntityType.TEXTBOOK);
    }

    @Test
    void entityListsAndFolderListsExposeReadyFilesOnly() {
        UUID readyId = UUID.randomUUID();
        UUID pendingId = UUID.randomUUID();
        UUID failedId = UUID.randomUUID();
        UUID folderId = UUID.randomUUID();
        FileMetadata ready = file(readyId, FileUploadStatus.READY, true, folderId);
        FileMetadata pending = file(pendingId, FileUploadStatus.PENDING, false, folderId);
        FileMetadata failed = file(failedId, FileUploadStatus.FAILED, false, folderId);
        given(fileMetadataRepository.findByEntityTypeAndEntityId(EntityType.TEXTBOOK, 9L))
                .willReturn(List.of(ready, pending, failed));
        given(fileMetadataRepository.findByEntityTypeAndFolderId(EntityType.TEXTBOOK, folderId))
                .willReturn(List.of(ready, pending, failed));
        given(fileMetadataRepository.findByEntityType(EntityType.TEXTBOOK))
                .willReturn(List.of(ready, pending, failed));

        assertThat(queryService.getFilesByEntity(EntityType.TEXTBOOK, 9L))
                .extracting(FileMetadataResponse::id)
                .containsExactly(readyId);
        assertThat(queryService.getFilesByEntityAndFolder(EntityType.TEXTBOOK, folderId))
                .extracting(FileMetadataResponse::id)
                .containsExactly(readyId);
        assertThat(queryService.getAllFilesByEntityType(EntityType.TEXTBOOK))
                .extracting(FileMetadataResponse::id)
                .containsExactly(readyId);
    }

    @Test
    void metadataLookupDoesNotExposePendingFile() {
        UUID pendingId = UUID.randomUUID();
        given(fileMetadataRepository.findById(pendingId))
                .willReturn(Optional.of(file(pendingId, FileUploadStatus.PENDING, false, null)));

        assertThatThrownBy(() -> queryService.getFileMetadata(pendingId))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.FILE_NOT_FOUND));
    }

    private static FileMetadata file(UUID id, FileUploadStatus status, boolean completed, UUID folderId) {
        FileMetadata metadata = FileMetadata.createWithId(id, EntityType.TEXTBOOK, null,
                "notes.pdf", "application/pdf", 10L, "files/demo/notes.pdf", folderId);
        ReflectionTestUtils.setField(metadata, "uploadStatus", status);
        if (completed) metadata.markUploadCompleted();
        return metadata;
    }
}
