package com.lumie.notification.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lumie.common.auth.Role;
import com.lumie.common.exception.GlobalExceptionHandler;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.notification.application.dto.request.UpsertSmsSenderSettingRequest;
import com.lumie.notification.application.dto.response.SmsSenderSettingResponse;
import com.lumie.notification.application.dto.response.SmsSenderSettingStatusResponse;
import com.lumie.notification.application.service.SmsSenderSettingService;
import com.lumie.notification.domain.vo.SmsSenderSettingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("SmsSenderSettingController 테스트")
class SmsSenderSettingControllerTest {

    @Mock
    private SmsSenderSettingService senderSettingService;

    @InjectMocks
    private SmsSenderSettingController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        UserContextHolder.setUserRole(Role.OWNER);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("GET /v1/sms/sender-setting 은 현재 설정을 반환한다")
    void getSenderSetting_returns200() throws Exception {
        given(senderSettingService.getSenderSetting()).willReturn(response());

        mockMvc.perform(get("/v1/sms/sender-setting"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("solapi"))
                .andExpect(jsonPath("$.registrationStatus").value("APPROVED"));
    }

    @Test
    @DisplayName("GET /v1/sms/sender-setting 은 OWNER가 아니면 거절한다")
    void getSenderSetting_nonOwner_returns403() throws Exception {
        UserContextHolder.setUserRole(Role.MANAGER);

        mockMvc.perform(get("/v1/sms/sender-setting"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /v1/sms/sender-setting/status 는 최소 상태만 반환한다")
    void getSenderSettingStatus_returns200() throws Exception {
        given(senderSettingService.getSenderSettingStatus())
                .willReturn(SmsSenderSettingStatusResponse.of("solapi", SmsSenderSettingStatus.PENDING));

        mockMvc.perform(get("/v1/sms/sender-setting/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("solapi"))
                .andExpect(jsonPath("$.registrationStatus").value("PENDING"))
                .andExpect(jsonPath("$.senderNumber").doesNotExist())
                .andExpect(jsonPath("$.rejectionReason").doesNotExist());
    }

    @Test
    @DisplayName("GET /v1/sms/sender-setting 은 미등록 상태를 반환한다")
    void getSenderSetting_withoutRow_returnsNotConfigured() throws Exception {
        given(senderSettingService.getSenderSetting())
                .willReturn(SmsSenderSettingResponse.notConfigured("solapi"));

        mockMvc.perform(get("/v1/sms/sender-setting"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("solapi"))
                .andExpect(jsonPath("$.senderNumber").value(nullValue()))
                .andExpect(jsonPath("$.registrationStatus").value("NOT_CONFIGURED"));
    }

    @Test
    @DisplayName("PUT /v1/sms/sender-setting 은 설정을 저장한다")
    void putSenderSetting_returns200() throws Exception {
        given(senderSettingService.upsertSenderSetting(any(UpsertSmsSenderSettingRequest.class))).willReturn(response());

        mockMvc.perform(put("/v1/sms/sender-setting")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpsertSmsSenderSettingRequest("021-234-5678"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderNumber").value("021-234-5678"));
    }

    private SmsSenderSettingResponse response() {
        return new SmsSenderSettingResponse(
                1L,
                "solapi",
                "021-234-5678",
                SmsSenderSettingStatus.APPROVED,
                null,
                Instant.parse("2026-06-28T00:00:00Z"),
                null,
                Instant.parse("2026-06-27T00:00:00Z"),
                Instant.parse("2026-06-28T00:00:00Z")
        );
    }
}
