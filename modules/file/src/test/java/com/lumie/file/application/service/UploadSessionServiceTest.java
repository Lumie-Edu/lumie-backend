package com.lumie.file.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.lumie.common.auth.Role;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.file.application.dto.request.CreateUploadSessionRequest;
import com.lumie.file.application.port.out.StoragePort;
import com.lumie.file.domain.vo.EntityType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UploadSessionServiceTest {

    @Mock
    private StoragePort storage;

    @Mock
    private UploadSessionReservationService reservationService;

    @Mock
    private UploadSessionStateService stateService;

    @InjectMocks
    private UploadSessionService service;

    @AfterEach
    void clearUserContext() {
        UserContextHolder.clear();
        TenantContextHolder.clear();
    }

    @Test
    void rejectsFileLargerThanUploadPolicyLimit() {
        var request = new CreateUploadSessionRequest(
                EntityType.ANNOUNCEMENT,
                "notes.pdf",
                "application/pdf",
                TemporaryUploadPolicy.MAX_BYTES + 1);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsFilenameOutsideUploadPolicy() {
        var request = new CreateUploadSessionRequest(
                EntityType.ANNOUNCEMENT,
                "notes.exe",
                "application/pdf",
                12L);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void reservesAndReturnsUploadSessionForValidFile() {
        TenantContextHolder.setTenant("test-tenant");
        TenantContextHolder.setTenantId(1L);
        UserContextHolder.setUserRole(Role.OWNER);
        var request = new CreateUploadSessionRequest(
                EntityType.ANNOUNCEMENT,
                "notes.pdf",
                "application/pdf",
                12L);
        given(storage.generatePresignedUploadUrl(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq("application/pdf"),
                org.mockito.ArgumentMatchers.eq(900)))
                .willReturn("https://objects.example.test/upload");

        var response = service.create(request);

        assertThat(response.uploadUrl()).isEqualTo("https://objects.example.test/upload");
        assertThat(response.objectKey()).endsWith("/notes.pdf");
        assertThat(response.status()).isEqualTo("ISSUED");
    }
}
