package com.lumie.homepage.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lumie.common.exception.GlobalExceptionHandler;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.homepage.application.dto.request.UpdateHomepageConfigRequest;
import com.lumie.homepage.application.dto.response.HomepageConfigResponse;
import com.lumie.homepage.application.service.HomepageCommandService;
import com.lumie.homepage.application.service.HomepageQueryService;
import com.lumie.common.exception.BusinessException;
import com.lumie.homepage.domain.exception.HomepageErrorCode;
import com.lumie.homepage.domain.vo.TemplateId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class HomepageControllerTest {

    private MockMvc mockMvc;

    @Mock
    private HomepageQueryService homepageQueryService;

    @Mock
    private HomepageCommandService homepageCommandService;

    @InjectMocks
    private HomepageController homepageController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(homepageController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        UserContextHolder.setUserRole("OWNER");
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("GET /v1/homepage - 설정 있을 때 반환")
    void getCurrent_returnsConfig() throws Exception {
        HomepageConfigResponse response = buildResponse(true);
        when(homepageQueryService.getCurrent()).thenReturn(Optional.of(response));

        mockMvc.perform(get("/v1/homepage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.templateId").value("solo-instructor"))
                .andExpect(jsonPath("$.published").value(true));
    }

    @Test
    @DisplayName("GET /v1/homepage - 미공개 시 404 반환")
    void getCurrent_returnsNullBody_whenNoConfig() throws Exception {
        when(homepageQueryService.getCurrent()).thenReturn(Optional.empty());

        mockMvc.perform(get("/v1/homepage"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /v1/homepage - OWNER 권한으로 설정 업데이트 성공")
    void update_success() throws Exception {
        HomepageConfigResponse response = buildResponse(false);
        when(homepageCommandService.update(any())).thenReturn(response);

        ObjectNode sections = objectMapper.createObjectNode();
        sections.put("hero", true);

        UpdateHomepageConfigRequest request = UpdateHomepageConfigRequest.builder()
                .templateId(TemplateId.SOLO_INSTRUCTOR)
                .templateVersion(1)
                .sections(sections)
                .build();

        mockMvc.perform(put("/v1/homepage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.templateId").value("solo-instructor"));
    }

    @Test
    @DisplayName("PUT /v1/homepage - OWNER 권한 없으면 403 반환")
    void update_returns_403_when_not_owner() throws Exception {
        // Override role to a non-owner value
        UserContextHolder.setUserRole("STUDENT");

        ObjectNode sections = objectMapper.createObjectNode();
        sections.put("hero", true);

        UpdateHomepageConfigRequest request = UpdateHomepageConfigRequest.builder()
                .templateId(TemplateId.SOLO_INSTRUCTOR)
                .templateVersion(1)
                .sections(sections)
                .build();

        mockMvc.perform(put("/v1/homepage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /v1/homepage/publish - 게시 상태 변경 성공")
    void publish_success() throws Exception {
        HomepageConfigResponse response = buildResponse(true);
        when(homepageCommandService.publish(any())).thenReturn(response);

        mockMvc.perform(post("/v1/homepage/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"published\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(true));
    }

    @Test
    @DisplayName("POST /v1/homepage/publish - 빈 config이면 422 반환")
    void publish_returns_422_when_not_ready() throws Exception {
        when(homepageCommandService.publish(any()))
                .thenThrow(new BusinessException(HomepageErrorCode.NOT_READY_TO_PUBLISH));

        mockMvc.perform(post("/v1/homepage/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"published\": true}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("HOMEPAGE_004"));
    }

    @Test
    @DisplayName("GET /v1/homepage/public/by-custom-id/{customId} - 공개 설정 반환")
    void getPublicByCustomId_returnsPublishedConfig() throws Exception {
        HomepageConfigResponse response = buildResponse(true);
        when(homepageQueryService.getPublicByCustomId("my-academy")).thenReturn(Optional.of(response));

        mockMvc.perform(get("/v1/homepage/public/by-custom-id/my-academy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(true));
    }

    @Test
    @DisplayName("GET /v1/homepage/public/by-custom-id/{customId} - 미공개 시 404 반환")
    void getPublicByCustomId_returnsNullBody_whenUnpublished() throws Exception {
        when(homepageQueryService.getPublicByCustomId("my-academy")).thenReturn(Optional.empty());

        mockMvc.perform(get("/v1/homepage/public/by-custom-id/my-academy"))
                .andExpect(status().isNotFound());
    }

    private HomepageConfigResponse buildResponse(boolean published) {
        ObjectNode sectionsNode = objectMapper.createObjectNode();
        return HomepageConfigResponse.builder()
                .id(1L)
                .templateId(TemplateId.SOLO_INSTRUCTOR)
                .templateVersion(1)
                .published(published)
                .sections(sectionsNode)
                .updatedAt(Instant.now())
                .build();
    }
}
