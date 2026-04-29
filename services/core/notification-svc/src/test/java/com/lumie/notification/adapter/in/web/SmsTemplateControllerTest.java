package com.lumie.notification.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lumie.notification.application.dto.CreateTemplateRequest;
import com.lumie.notification.application.dto.SmsTemplateResponse;
import com.lumie.notification.application.dto.UpdateTemplateRequest;
import com.lumie.notification.application.service.SmsCommandService;
import com.lumie.notification.application.service.SmsQueryService;
import com.lumie.notification.domain.exception.SmsTemplateNotFoundException;
import com.lumie.notification.domain.vo.TemplateCategory;
import com.lumie.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SmsTemplateController 테스트")
class SmsTemplateControllerTest {

    @Mock private SmsCommandService commandService;
    @Mock private SmsQueryService queryService;
    @InjectMocks private SmsTemplateController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("GET /v1/sms/templates - 템플릿 목록 조회 시 200을 반환한다")
    void listTemplates_returns200() throws Exception {
        SmsTemplateResponse response = buildTemplateResponse(1L);
        Page<SmsTemplateResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1);

        given(queryService.listTemplates(any())).willReturn(page);

        mockMvc.perform(get("/v1/sms/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("출석 알림"));
    }

    @Test
    @DisplayName("POST /v1/sms/templates - 템플릿 생성 시 201을 반환한다")
    void createTemplate_returns201() throws Exception {
        SmsTemplateResponse response = buildTemplateResponse(1L);
        given(commandService.createTemplate(any(CreateTemplateRequest.class))).willReturn(response);

        CreateTemplateRequest request = new CreateTemplateRequest(
                "출석 알림", "{학생명}님이 출석했습니다.", TemplateCategory.ATTENDANCE);

        mockMvc.perform(post("/v1/sms/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("출석 알림"));
    }

    @Test
    @DisplayName("PATCH /v1/sms/templates/{id} - 템플릿 수정 시 200을 반환한다")
    void updateTemplate_returns200() throws Exception {
        SmsTemplateResponse response = buildTemplateResponse(1L);
        given(commandService.updateTemplate(eq(1L), any(UpdateTemplateRequest.class))).willReturn(response);

        UpdateTemplateRequest request = new UpdateTemplateRequest("변경된 이름", null, null);

        mockMvc.perform(patch("/v1/sms/templates/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("DELETE /v1/sms/templates/{id} - 템플릿 삭제 시 204를 반환한다")
    void deleteTemplate_returns204() throws Exception {
        doNothing().when(commandService).deleteTemplate(1L);

        mockMvc.perform(delete("/v1/sms/templates/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /v1/sms/templates/{id} - 존재하지 않는 템플릿 삭제 시 404를 반환한다")
    void deleteTemplate_notFound_returns404() throws Exception {
        doThrow(new SmsTemplateNotFoundException(999L)).when(commandService).deleteTemplate(999L);

        mockMvc.perform(delete("/v1/sms/templates/999"))
                .andExpect(status().isNotFound());
    }

    private SmsTemplateResponse buildTemplateResponse(Long id) {
        return new SmsTemplateResponse(
                id, "출석 알림", "{학생명}님이 출석했습니다.",
                TemplateCategory.ATTENDANCE,
                LocalDateTime.now(), LocalDateTime.now());
    }
}
