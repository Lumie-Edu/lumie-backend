package com.lumie.file.application.service;

import com.lumie.common.auth.Role;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.file.application.port.out.StoragePort;
import com.lumie.file.domain.entity.FileDownload;
import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.repository.FileDownloadRepository;
import com.lumie.file.domain.repository.FileMetadataRepository;
import com.lumie.file.domain.exception.FileErrorCode;
import com.lumie.file.domain.vo.EntityType;
import com.lumie.student.api.StudentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
}
