package com.lumie.file.application.service;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.file.application.dto.request.CreateTemporaryUploadRequest;
import com.lumie.file.application.port.out.StoragePort;
import com.lumie.file.domain.vo.EntityType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TemporaryObjectUploadServiceTest {

    private final StoragePort storage = mock(StoragePort.class);
    private final TemporaryObjectUploadService service = new TemporaryObjectUploadService(storage);

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenant("inst-demo");
        TenantContextHolder.setTenantId(42L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void rejectsZeroByteAttachmentBeforeGeneratingUploadUrl() {
        assertThatThrownBy(() -> service.create(request("notes.pdf", "application/pdf", 0L)))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void rejectsNegativeAttachmentSizeBeforeGeneratingUploadUrl() {
        assertThatThrownBy(() -> service.create(request("notes.pdf", "application/pdf", -1L)))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void rejectsBlankContentTypeBeforeGeneratingUploadUrl() {
        assertThatThrownBy(() -> service.create(request("notes.pdf", "  ", 12L)))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void rejectsFilenameWithoutSupportedExtensionBeforeGeneratingUploadUrl() {
        assertThatThrownBy(() -> service.create(request("notes.exe", "application/octet-stream", 12L)))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void acceptsMaximumSizeAndCaseInsensitiveAllowedExtension() {
        when(storage.generatePresignedUploadUrl(anyString(), eq("application/pdf"), eq(900)))
                .thenReturn("signed-url");

        var response = service.create(request("Notes.PDF", "application/pdf", TemporaryObjectUploadService.MAX_BYTES));

        assertThat(response.uploadUrl()).isEqualTo("signed-url");
        assertThat(response.objectKey()).startsWith("tmp/");
        verify(storage).generatePresignedUploadUrl(anyString(), eq("application/pdf"), eq(900));
    }

    @Test
    void rejectsSizeAboveLimitAndUnsupportedEntityType() {
        assertThatThrownBy(() -> service.create(request("notes.pdf", "application/pdf",
                TemporaryObjectUploadService.MAX_BYTES + 1)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.create(new CreateTemporaryUploadRequest(
                EntityType.OMR, "notes.pdf", "application/pdf", 10L)))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void rejectsMalformedFilenamesWithoutDotsOrWithPathTraversal() {
        assertThat(TemporaryObjectUploadService.isAllowedFilename("notes")).isFalse();
        assertThat(TemporaryObjectUploadService.isAllowedFilename("../notes.pdf")).isFalse();
        assertThat(TemporaryObjectUploadService.isAllowedFilename("folder\\notes.pdf")).isFalse();
        assertThat(TemporaryObjectUploadService.isAllowedFilename(null)).isFalse();
    }

    @Test
    void rejectsMissingRequiredUploadFieldsWithoutTouchingStorage() {
        assertThatThrownBy(() -> service.create(null)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.create(new CreateTemporaryUploadRequest(null, "notes.pdf", "application/pdf", 10L)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.create(new CreateTemporaryUploadRequest(EntityType.ANNOUNCEMENT,
                null, "application/pdf", 10L))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.create(new CreateTemporaryUploadRequest(EntityType.ANNOUNCEMENT,
                "notes.pdf", null, 10L))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.create(new CreateTemporaryUploadRequest(EntityType.ANNOUNCEMENT,
                "notes.pdf", "application/pdf", null))).isInstanceOf(BusinessException.class);
        verifyNoInteractions(storage);
    }

    private static CreateTemporaryUploadRequest request(String filename, String contentType, Long size) {
        return new CreateTemporaryUploadRequest(EntityType.ANNOUNCEMENT, filename, contentType, size);
    }
}
