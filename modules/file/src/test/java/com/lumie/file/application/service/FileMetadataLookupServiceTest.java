package com.lumie.file.application.service;

import com.lumie.common.auth.Role;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.content.api.ContentService;
import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.exception.FileErrorCode;
import com.lumie.file.domain.repository.FileMetadataRepository;
import com.lumie.file.domain.vo.EntityType;
import com.lumie.file.domain.vo.FileUploadStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileMetadataLookupServiceTest {

    @Mock private FileMetadataRepository repository;
    @Mock private ContentService contentService;
    @InjectMocks private FileMetadataLookupService lookup;

    @AfterEach
    void clearContext() {
        UserContextHolder.clear();
    }

    @Test
    void studentCanReadVisibleAnnouncementMetadata() {
        UserContextHolder.setUserRole(Role.STUDENT);
        UserContextHolder.setUserId(42L);
        when(contentService.canUserViewAnnouncement(7L, "STUDENT", 42L)).thenReturn(true);

        lookup.requireAccess(EntityType.ANNOUNCEMENT, 7L);

        verify(contentService).canUserViewAnnouncement(7L, "STUDENT", 42L);
    }

    @Test
    void studentCanReadVisibleQnaMetadata() {
        UserContextHolder.setUserRole(Role.STUDENT);
        UserContextHolder.setUserId(42L);
        when(contentService.canUserViewQna(8L, "STUDENT", 42L)).thenReturn(true);

        lookup.requireAccess(EntityType.QNA, 8L);

        verify(contentService).canUserViewQna(8L, "STUDENT", 42L);
    }

    @Test
    void requestWithoutUserRoleIsForbiddenBeforeContentAuthorization() {
        assertThatThrownBy(() -> lookup.requireAccess(EntityType.ANNOUNCEMENT, 7L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isEqualTo(com.lumie.common.exception.CommonErrorCode.FORBIDDEN));
        verifyNoInteractions(contentService);
    }

    @Test
    void studentCannotReadAnotherStudentsQnaMetadata() {
        UserContextHolder.setUserRole(Role.STUDENT);
        UserContextHolder.setUserId(42L);

        assertThatThrownBy(() -> lookup.requireAccess(EntityType.QNA, 8L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isEqualTo(FileErrorCode.ACCESS_DENIED));
        verify(contentService).canUserViewQna(8L, "STUDENT", 42L);
    }

    @Test
    void studentCannotEnumerateCollectionsWithoutAVisibleParent() {
        UserContextHolder.setUserRole(Role.STUDENT);

        assertThatThrownBy(() -> lookup.requireAccess(EntityType.ANNOUNCEMENT, null))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> lookup.requireAccess(EntityType.TEXTBOOK, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void staffRetainTextbookMetadataAccess() {
        UserContextHolder.setUserRole(Role.INSTRUCTOR);

        lookup.requireAccess(EntityType.TEXTBOOK, null);
    }

    @Test
    void studentCannotDownloadTextbookEvenWithItsFileId() {
        UUID fileId = UUID.randomUUID();
        FileMetadata textbook = FileMetadata.createWithId(fileId, EntityType.TEXTBOOK, null,
                "textbook.pdf", "application/pdf", 123L, "textbook/object", null);
        textbook.markUploadCompleted();
        UserContextHolder.setUserRole(Role.STUDENT);
        UserContextHolder.setUserId(42L);
        when(repository.findById(fileId)).thenReturn(Optional.of(textbook));

        assertThatThrownBy(() -> lookup.getAuthorizedDownloadMetadata(fileId))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isEqualTo(FileErrorCode.ACCESS_DENIED));
    }

    @Test
    void missingMetadataIsNotFoundBeforeDownloadAuthorization() {
        UUID fileId = UUID.randomUUID();
        when(repository.findById(fileId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lookup.getAuthorizedDownloadMetadata(fileId))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isEqualTo(FileErrorCode.FILE_NOT_FOUND));
        verifyNoInteractions(contentService);
    }

    @Test
    void metadataMarkedCompletedButStillPendingCannotBeDownloaded() {
        UUID fileId = UUID.randomUUID();
        FileMetadata inconsistent = FileMetadata.createWithId(fileId, EntityType.TEXTBOOK, null,
                "textbook.pdf", "application/pdf", 123L, "textbook/object", null);
        inconsistent.markUploadCompleted();
        ReflectionTestUtils.setField(inconsistent, "uploadStatus", FileUploadStatus.PENDING);
        UserContextHolder.setUserRole(Role.INSTRUCTOR);
        when(repository.findById(fileId)).thenReturn(Optional.of(inconsistent));

        assertThatThrownBy(() -> lookup.getAuthorizedDownloadMetadata(fileId))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isEqualTo(FileErrorCode.FILE_NOT_FOUND));
    }
}
