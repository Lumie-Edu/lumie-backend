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
import com.lumie.file.application.service.TemporaryObjectUploadService;
import com.lumie.file.application.service.TextbookFolderCommandService;
import com.lumie.file.application.service.TextbookFolderQueryService;
import com.lumie.file.application.dto.request.CreateTemporaryUploadRequest;
import com.lumie.file.application.dto.response.TemporaryObjectUploadResponse;
import com.lumie.file.domain.vo.EntityType;
import java.util.UUID;
import java.util.List;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
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
    @Mock private TemporaryObjectUploadService temporaryObjectUploadService;
    @Mock private TextbookFolderCommandService textbookFolderCommandService;
    @Mock private TextbookFolderQueryService textbookFolderQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new FileController(fileCommandService, fileQueryService, temporaryObjectUploadService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void clearUserContext() {
        UserContextHolder.clear();
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
    void studentCannotUseFileDownloaderOrTextbookRoutesBeforeServices() {
        UserContextHolder.setUserRole(Role.STUDENT);
        FileController controller = new FileController(fileCommandService, fileQueryService, temporaryObjectUploadService);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.getFileDownloaders(UUID.randomUUID()))
                .isInstanceOf(com.lumie.common.exception.BusinessException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.registerTextbookFiles(null))
                .isInstanceOf(com.lumie.common.exception.BusinessException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.getFilesByEntityType(
                        EntityType.TEXTBOOK, 1L, null, false))
                .isInstanceOf(com.lumie.common.exception.BusinessException.class);

        verifyNoInteractions(fileQueryService, fileCommandService);
    }

    @Test
    void studentCannotUploadWhenAuthenticatedRoleIsMissing() {
        UserContextHolder.clear();
        CreateTemporaryUploadRequest request = new CreateTemporaryUploadRequest(
                EntityType.QNA, "question.pdf", "application/pdf", 12L);
        FileController controller = new FileController(fileCommandService, fileQueryService, temporaryObjectUploadService);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.createTemporaryUpload(request))
                .isInstanceOf(com.lumie.common.exception.BusinessException.class);

        verifyNoInteractions(temporaryObjectUploadService);
    }

    @Test
    void studentMayRequestOnlyQnaAttachments() {
        UserContextHolder.setUserRole(Role.STUDENT);
        CreateTemporaryUploadRequest qnaRequest = new CreateTemporaryUploadRequest(
                EntityType.QNA, "question.pdf", "application/pdf", 12L);
        given(temporaryObjectUploadService.create(qnaRequest))
                .willReturn(new TemporaryObjectUploadResponse("qna/tmp/file", "https://upload.example.test"));
        FileController controller = new FileController(fileCommandService, fileQueryService, temporaryObjectUploadService);

        var response = controller.createTemporaryUpload(qnaRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        verify(temporaryObjectUploadService).create(qnaRequest);

        CreateTemporaryUploadRequest textbookRequest = new CreateTemporaryUploadRequest(
                EntityType.TEXTBOOK, "book.pdf", "application/pdf", 12L);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.createTemporaryUpload(textbookRequest))
                .isInstanceOf(com.lumie.common.exception.BusinessException.class);
        verify(temporaryObjectUploadService, never()).create(textbookRequest);
    }

    @Test
    void studentCannotListTextbookFoldersBeforeQueryService() throws Exception {
        UserContextHolder.setUserRole(Role.STUDENT);
        MockMvc folderMvc = MockMvcBuilders.standaloneSetup(
                        new TextbookFolderController(textbookFolderCommandService, textbookFolderQueryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        folderMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                        "/v1/textbook-folders"))
                .andExpect(status().isForbidden());

        verify(textbookFolderQueryService, never()).listFolders(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void studentIsDeniedAtEntryToEveryTextbookFolderHandler() throws Exception {
        UserContextHolder.setUserRole(Role.STUDENT);
        TextbookFolderController controller = new TextbookFolderController(
                textbookFolderCommandService, textbookFolderQueryService);

        for (Method handler : Arrays.stream(TextbookFolderController.class.getDeclaredMethods())
                .filter(FileRoleAuthorizationTest::isMappedHandler)
                .toList()) {
            try {
                handler.invoke(controller, new Object[handler.getParameterCount()]);
                throw new AssertionError("Expected 403 at route entry for " + handler.getName());
            } catch (InvocationTargetException exception) {
                org.assertj.core.api.Assertions.assertThat(exception.getCause())
                        .isInstanceOf(com.lumie.common.exception.BusinessException.class);
            }
        }

        verifyNoInteractions(textbookFolderCommandService, textbookFolderQueryService);
    }

    @Test
    void instructorCanListTextbookFolders() throws Exception {
        UserContextHolder.setUserRole(Role.INSTRUCTOR);
        given(textbookFolderQueryService.listFolders(null)).willReturn(List.of());
        MockMvc folderMvc = MockMvcBuilders.standaloneSetup(
                        new TextbookFolderController(textbookFolderCommandService, textbookFolderQueryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        folderMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                        "/v1/textbook-folders"))
                .andExpect(status().isOk());

        verify(textbookFolderQueryService).listFolders(null);
    }

    @Test
    void instructorCanEnterEveryTextbookFolderHandler() throws Exception {
        UserContextHolder.setUserRole(Role.INSTRUCTOR);
        given(textbookFolderQueryService.listFolders(org.mockito.ArgumentMatchers.any())).willReturn(List.of());
        TextbookFolderController controller = new TextbookFolderController(
                textbookFolderCommandService, textbookFolderQueryService);

        for (Method handler : Arrays.stream(TextbookFolderController.class.getDeclaredMethods())
                .filter(FileRoleAuthorizationTest::isMappedHandler)
                .toList()) {
            Object result = handler.invoke(controller, folderArguments(handler));
            assertThat(result)
                    .as("route handler %s", handler.getName())
                    .isInstanceOf(org.springframework.http.ResponseEntity.class);
        }
    }

    private static Object[] folderArguments(Method method) {
        Class<?>[] types = method.getParameterTypes();
        Object[] args = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            if (types[i] == UUID.class) args[i] = UUID.randomUUID();
            else if (types[i].getName().contains(".dto.request.")) args[i] = org.mockito.Mockito.mock(types[i]);
        }
        return args;
    }

    private static boolean isMappedHandler(Method method) {
        return Arrays.stream(method.getDeclaredAnnotations())
                .anyMatch(annotation -> annotation.annotationType().getSimpleName().endsWith("Mapping"));
    }

    @Test
    void instructorCanRequestEverySupportedUploadEntityType() {
        FileController controller = new FileController(fileCommandService, fileQueryService, temporaryObjectUploadService);
        for (Role role : List.of(Role.OWNER, Role.MANAGER, Role.INSTRUCTOR)) {
            UserContextHolder.setUserRole(role);
            for (EntityType entityType : List.of(EntityType.ANNOUNCEMENT, EntityType.QNA, EntityType.TEXTBOOK)) {
                CreateTemporaryUploadRequest request = new CreateTemporaryUploadRequest(
                        entityType, role.name().toLowerCase(Locale.ROOT) + "-file.pdf", "application/pdf", 12L);
                given(temporaryObjectUploadService.create(request))
                        .willReturn(new TemporaryObjectUploadResponse("tmp/object", "https://upload.example.test"));

                assertThat(controller.createTemporaryUpload(request).getStatusCode().value()).isEqualTo(201);
                verify(temporaryObjectUploadService).create(request);
            }
        }
    }

    @Test
    void instructorCanUseTextbookFileManagementRoutes() {
        UserContextHolder.setUserRole(Role.INSTRUCTOR);
        UUID fileId = UUID.randomUUID();
        given(fileQueryService.getFileDownloaders(fileId)).willReturn(List.of());
        given(fileCommandService.registerTextbookFiles(
                        org.mockito.ArgumentMatchers.any(com.lumie.file.application.dto.request.RegisterTextbookFilesRequest.class)))
                .willReturn(List.of());
        given(fileQueryService.getFilesByEntity(EntityType.TEXTBOOK, 1L)).willReturn(List.of());
        FileController controller = new FileController(fileCommandService, fileQueryService, temporaryObjectUploadService);

        assertThat(controller.getFileDownloaders(fileId).getStatusCode().value()).isEqualTo(200);
        assertThat(controller.registerTextbookFiles(
                        org.mockito.Mockito.mock(com.lumie.file.application.dto.request.RegisterTextbookFilesRequest.class))
                .getStatusCode().value()).isEqualTo(201);
        assertThat(controller.getFilesByEntityType(EntityType.TEXTBOOK, 1L, null, false).getStatusCode().value())
                .isEqualTo(200);
    }

    @Test
    void instructorCanUseEachFileListingMode() {
        UserContextHolder.setUserRole(Role.INSTRUCTOR);
        UUID folderId = UUID.randomUUID();
        given(fileQueryService.getAllFilesByEntityType(EntityType.TEXTBOOK)).willReturn(List.of());
        given(fileQueryService.getFilesByEntityAndFolder(EntityType.TEXTBOOK, folderId)).willReturn(List.of());
        given(fileQueryService.getFilesByEntity(EntityType.TEXTBOOK, 1L)).willReturn(List.of());
        given(fileQueryService.getFilesByEntity(EntityType.ANNOUNCEMENT, 2L)).willReturn(List.of());
        FileController controller = new FileController(fileCommandService, fileQueryService, temporaryObjectUploadService);

        controller.getFilesByEntityType(EntityType.TEXTBOOK, null, null, true);
        controller.getFilesByEntityType(EntityType.TEXTBOOK, null, folderId, false);
        controller.getFilesByEntityType(EntityType.TEXTBOOK, 1L, null, false);
        controller.getFilesByEntityType(EntityType.ANNOUNCEMENT, 2L, null, false);

        verify(fileQueryService).getAllFilesByEntityType(EntityType.TEXTBOOK);
        verify(fileQueryService).getFilesByEntityAndFolder(EntityType.TEXTBOOK, folderId);
        verify(fileQueryService).getFilesByEntity(EntityType.TEXTBOOK, 1L);
        verify(fileQueryService).getFilesByEntity(EntityType.ANNOUNCEMENT, 2L);
    }
}
