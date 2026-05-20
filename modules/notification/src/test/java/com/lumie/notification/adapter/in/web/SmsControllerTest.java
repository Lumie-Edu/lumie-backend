package com.lumie.notification.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lumie.notification.application.dto.request.SendSmsRequest;
import com.lumie.notification.application.dto.response.SmsMessageResponse;
import com.lumie.notification.application.service.SmsCommandService;
import com.lumie.notification.application.service.SmsQueryService;
import com.lumie.notification.domain.exception.SmsMessageNotFoundException;
import com.lumie.notification.domain.vo.Recipient;
import com.lumie.notification.domain.vo.RecipientType;
import com.lumie.notification.domain.vo.SmsStatus;
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

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SmsController 테스트")
class SmsControllerTest {

    @Mock private SmsCommandService commandService;
    @Mock private SmsQueryService queryService;
    @InjectMocks private SmsController controller;

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
    @DisplayName("POST /v1/sms/send - 문자 발송 시 201을 반환한다")
    void sendSms_returns201() throws Exception {
        SmsMessageResponse response = buildSmsResponse(1L);
        given(commandService.sendSms(any(SendSmsRequest.class))).willReturn(response);

        SendSmsRequest request = new SendSmsRequest(
                10L, RecipientType.STUDENT,
                List.of(new Recipient("010-1234-5678", "홍길동", 100L)),
                "제목", "내용", null);

        mockMvc.perform(post("/v1/sms/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    @DisplayName("GET /v1/sms/history - 발송 이력 조회 시 200을 반환한다")
    void listMessages_returns200() throws Exception {
        SmsMessageResponse response = buildSmsResponse(1L);
        Page<SmsMessageResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1);

        given(queryService.listMessages(any(), any(), any(), any())).willReturn(page);

        mockMvc.perform(get("/v1/sms/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].status").value("SENT"));
    }

    @Test
    @DisplayName("GET /v1/sms/history/{id} - 문자 상세 조회 시 200을 반환한다")
    void getMessage_returns200() throws Exception {
        SmsMessageResponse response = buildSmsResponse(1L);
        given(queryService.getMessage(1L)).willReturn(response);

        mockMvc.perform(get("/v1/sms/history/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.content").value("테스트 내용"));
    }

    @Test
    @DisplayName("GET /v1/sms/history/{id} - 존재하지 않는 문자 조회 시 404를 반환한다")
    void getMessage_notFound_returns404() throws Exception {
        given(queryService.getMessage(999L)).willThrow(new SmsMessageNotFoundException(999L));

        mockMvc.perform(get("/v1/sms/history/999"))
                .andExpect(status().isNotFound());
    }

    private SmsMessageResponse buildSmsResponse(Long id) {
        return new SmsMessageResponse(
                id, 10L, RecipientType.STUDENT,
                List.of(new Recipient("010-1234-5678", "홍길동", 100L)),
                "테스트 제목", "테스트 내용", null,
                Instant.now(), SmsStatus.SENT, 1, 0,
                Instant.now(), Instant.now());
    }
}
