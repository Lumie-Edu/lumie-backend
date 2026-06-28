package com.lumie.notification.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lumie.common.exception.GlobalExceptionHandler;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.notification.application.dto.request.UpdateSmsSenderSettingApprovalRequest;
import com.lumie.notification.application.dto.response.SmsSenderSettingResponse;
import com.lumie.notification.application.service.SmsSenderSettingService;
import com.lumie.notification.domain.vo.SmsSenderSettingStatus;
import com.lumie.tenant.api.TenantService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("InternalSmsSenderSettingController 테스트")
class InternalSmsSenderSettingControllerTest {

    @Mock
    private SmsSenderSettingService senderSettingService;

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private InternalSmsSenderSettingController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("PATCH /internal/sms/sender-settings/{tenantSlug}/approval 은 tenant context 안에서 승인 상태를 갱신한다")
    void patchApproval_returns200() throws Exception {
        given(tenantService.getTenantBySlug("inst-test")).willReturn(Optional.of(new TenantService.TenantData(
                1L,
                "inst-test",
                "테스트 학원",
                com.lumie.tenant.api.TenantStatus.ACTIVE,
                null,
                null,
                null,
                null
        )));
        given(senderSettingService.updateApproval(any(UpdateSmsSenderSettingApprovalRequest.class)))
                .willAnswer(inv -> {
                    assertThat(TenantContextHolder.getRequiredTenant()).isEqualTo("inst-test");
                    assertThat(TenantContextHolder.getRequiredTenantId()).isEqualTo(1L);
                    return response();
                });

        mockMvc.perform(patch("/internal/sms/sender-settings/inst-test/approval")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateSmsSenderSettingApprovalRequest(
                                UpdateSmsSenderSettingApprovalRequest.ApprovalStatus.APPROVED,
                                null
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationStatus").value("APPROVED"));
    }

    @Test
    @DisplayName("PATCH approval 은 HMAC tenant와 path tenant가 다르면 거절한다")
    void patchApproval_withTenantMismatch_returns403() throws Exception {
        TenantContextHolder.setTenant("inst-other");
        TenantContextHolder.setTenantId(2L);

        mockMvc.perform(patch("/internal/sms/sender-settings/inst-test/approval")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateSmsSenderSettingApprovalRequest(
                                UpdateSmsSenderSettingApprovalRequest.ApprovalStatus.APPROVED,
                                null
                        ))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(tenantService);
        verifyNoInteractions(senderSettingService);
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
