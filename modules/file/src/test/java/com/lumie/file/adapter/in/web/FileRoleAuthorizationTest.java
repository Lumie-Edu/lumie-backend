package com.lumie.file.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lumie.common.auth.Role;
import com.lumie.common.exception.GlobalExceptionHandler;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.file.application.service.FileCommandService;
import com.lumie.file.application.service.FileQueryService;
import com.lumie.file.application.service.UploadSessionService;
import com.lumie.file.application.dto.response.FileDownloadResult;
import com.lumie.file.domain.vo.EntityType;
import java.util.UUID;
import java.util.List;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class FileRoleAuthorizationTest {

    @Mock private FileCommandService fileCommandService;
    @Mock private FileQueryService fileQueryService;
    @Mock private UploadSessionService uploadSessionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new FileController(fileCommandService, fileQueryService, uploadSessionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void clearUserContext() {
        UserContextHolder.clear();
    }

    @Test
    void downloadsAreRecordedForEveryAuthenticatedRole() {
        UUID fileId = UUID.randomUUID();
        var controller = new FileController(fileCommandService, fileQueryService, uploadSessionService);
        given(fileQueryService.downloadFile(fileId)).willReturn(
                new FileDownloadResult(new ByteArrayInputStream(new byte[0]), "test.pdf", "application/pdf", 0));
        UserContextHolder.setUserId(77L);
        for (Role role : Role.values()) {
            UserContextHolder.setUserRole(role);
            controller.downloadFile(fileId);
        }

        verify(fileCommandService, org.mockito.Mockito.times(Role.values().length))
                .recordDownload(fileId, 77L);
    }

    @Test
    void downloadIsNotRecordedWithoutAnAuthenticatedUserContext() {
        UUID fileId = UUID.randomUUID();
        var controller = new FileController(fileCommandService, fileQueryService, uploadSessionService);
        given(fileQueryService.downloadFile(fileId)).willReturn(
                new FileDownloadResult(new ByteArrayInputStream(new byte[0]), "test.pdf", "application/pdf", 0));

        UserContextHolder.setUserRole(Role.OWNER);
        controller.downloadFile(fileId);

        UserContextHolder.clear();
        controller.downloadFile(fileId);

        UserContextHolder.setUserId(77L);
        UserContextHolder.setUserRole(null);
        controller.downloadFile(fileId);

        verifyNoInteractions(fileCommandService);
    }

    @Test
    void studentCannotDeleteAnyFileBeforeCommandService() throws Exception {
        UserContextHolder.setUserRole(Role.STUDENT);
        UUID fileId = UUID.randomUUID();

        mockMvc.perform(delete("/v1/files/{fileId}", fileId))
                .andExpect(status().isForbidden());

        verify(fileCommandService, never()).deleteFile(fileId);
    }

    @Test
    void instructorCanDeleteAnyTenantFileWithoutCreatorCheck() throws Exception {
        UserContextHolder.setUserRole(Role.INSTRUCTOR);
        UUID fileId = UUID.randomUUID();

        mockMvc.perform(delete("/v1/files/{fileId}", fileId))
                .andExpect(status().isNoContent());

        verify(fileCommandService).deleteFile(fileId);
    }

    @Test
    void ownerAndManagerCanDeleteTenantFiles() throws Exception {
        UUID fileId = UUID.randomUUID();
        for (Role role : List.of(Role.OWNER, Role.MANAGER)) {
            UserContextHolder.setUserRole(role);

            mockMvc.perform(delete("/v1/files/{fileId}", fileId))
                    .andExpect(status().isNoContent());
        }

        verify(fileCommandService, org.mockito.Mockito.times(2)).deleteFile(fileId);
    }

    @Test
    void authenticatedStaffGuardReturnsForbiddenWhenRoleIsMissing() throws Exception {
        UserContextHolder.clear();

        var response = mockMvc.perform(delete("/v1/files/{fileId}", UUID.randomUUID()))
                .andExpect(status().isForbidden())
                .andReturn();

        assertThat(response.getResponse().getContentAsString()).contains("COMMON_006");
        verify(fileCommandService, never()).deleteFile(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void studentCannotUseFileDownloaderBeforeServices() {
        UserContextHolder.setUserRole(Role.STUDENT);
        FileController controller = new FileController(fileCommandService, fileQueryService, uploadSessionService);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.getFileDownloaders(UUID.randomUUID()))
                .isInstanceOf(com.lumie.common.exception.BusinessException.class);
        verifyNoInteractions(fileQueryService, fileCommandService);
    }

    @Test
    void listsFilesForAnEntity() {
        Long entityId = 12L;
        given(fileQueryService.getFilesByEntity(EntityType.ANNOUNCEMENT, entityId)).willReturn(List.of());
        FileController controller = new FileController(fileCommandService, fileQueryService, uploadSessionService);

        var response = controller.getFilesByEntityType(EntityType.ANNOUNCEMENT, entityId);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(fileQueryService).getFilesByEntity(EntityType.ANNOUNCEMENT, entityId);
    }
}
