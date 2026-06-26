package com.lumie.notification.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lumie.notification.application.dto.request.CreateSmsRecipientGroupRequest;
import com.lumie.notification.application.dto.request.RecipientRequest;
import com.lumie.notification.application.dto.request.SendSmsRequest;
import com.lumie.notification.application.dto.request.SendToClassRequest;
import com.lumie.notification.application.dto.response.RecipientResponse;
import com.lumie.notification.application.dto.response.SmsMessageResponse;
import com.lumie.notification.application.dto.response.SmsRecipientGroupResponse;
import com.lumie.notification.application.dto.response.SmsRecipientGroupSummaryResponse;
import com.lumie.notification.application.service.SmsCommandService;
import com.lumie.notification.application.service.SmsQueryService;
import com.lumie.notification.application.service.SmsRecipientGroupService;
import com.lumie.common.idempotency.IdempotencyService;
import com.lumie.common.exception.BusinessException;
import com.lumie.notification.domain.exception.SmsErrorCode;
import com.lumie.notification.domain.vo.RecipientRole;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SmsController 테스트")
class SmsControllerTest {

    @Mock private SmsCommandService commandService;
    @Mock private SmsQueryService queryService;
    @Mock private SmsRecipientGroupService recipientGroupService;
    @Mock private IdempotencyService idempotencyService;
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
        given(idempotencyService.executeOnceWithoutResponseBody(anyString(), anyString(), any(), any()))
                .willAnswer(inv -> ((java.util.function.Supplier<?>) inv.getArgument(3)).get());

        SendSmsRequest request = new SendSmsRequest(
                List.of(new RecipientRequest("010-1234-5678", "홍길동", 100L, null, RecipientRole.GUARDIAN)),
                "제목", "내용", null);

        mockMvc.perform(post("/v1/sms/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "sms-test-key-0001")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    @DisplayName("POST /v1/sms/send - Idempotency-Key 누락 시 400을 반환한다")
    void sendSms_missingIdempotencyKey_returns400() throws Exception {
        SendSmsRequest request = new SendSmsRequest(
                List.of(new RecipientRequest("010-1234-5678", "홍길동", 100L, null, RecipientRole.GUARDIAN)),
                "제목", "내용", null);

        mockMvc.perform(post("/v1/sms/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /v1/sms/send - Idempotency-Key 형식 오류 시 400을 반환한다")
    void sendSms_invalidIdempotencyKey_returns400() throws Exception {
        SendSmsRequest request = new SendSmsRequest(
                List.of(new RecipientRequest("010-1234-5678", "홍길동", 100L, null, RecipientRole.GUARDIAN)),
                "제목", "내용", null);

        mockMvc.perform(post("/v1/sms/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "bad key")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /v1/sms/send - 수신자 역할 누락 시 400을 반환한다")
    void sendSms_missingRecipientRole_returns400() throws Exception {
        String request = """
                {
                  "recipients": [
                    {"phone": "010-1234-5678", "name": "홍길동", "studentId": 100}
                  ],
                  "content": "내용"
                }
                """;

        mockMvc.perform(post("/v1/sms/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "sms-test-key-0002")
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /v1/sms/recipient-groups - 수신 그룹 목록 조회 시 200을 반환한다")
    void listRecipientGroups_returns200() throws Exception {
        SmsRecipientGroupSummaryResponse response = buildRecipientGroupSummaryResponse(1L);
        given(recipientGroupService.listGroupSummaries()).willReturn(List.of(response));

        mockMvc.perform(get("/v1/sms/recipient-groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("레벨업학원 전체 학부모님"))
                .andExpect(jsonPath("$[0].recipientCount").value(1))
                .andExpect(jsonPath("$[0].recipients").doesNotExist());
    }

    @Test
    @DisplayName("GET /v1/sms/recipient-groups/{id} - 수신 그룹 상세 조회 시 200을 반환한다")
    void getRecipientGroup_returns200() throws Exception {
        SmsRecipientGroupResponse response = buildRecipientGroupResponse(1L);
        given(recipientGroupService.getGroup(1L)).willReturn(response);

        mockMvc.perform(get("/v1/sms/recipient-groups/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.recipients[0].phone").value("010-****-5678"));
    }

    @Test
    @DisplayName("POST /v1/sms/recipient-groups - 수신 그룹 생성 시 201을 반환한다")
    void createRecipientGroup_returns201() throws Exception {
        SmsRecipientGroupResponse response = buildRecipientGroupResponse(1L);
        given(recipientGroupService.createGroup(any(CreateSmsRecipientGroupRequest.class))).willReturn(response);
        given(idempotencyService.executeOnceWithoutResponseBody(anyString(), anyString(), any(), any()))
                .willAnswer(inv -> ((java.util.function.Supplier<?>) inv.getArgument(3)).get());

        CreateSmsRecipientGroupRequest request = new CreateSmsRecipientGroupRequest(
                "레벨업학원 전체 학부모님",
                "mic",
                List.of(new RecipientRequest("010-1234-5678", "홍길동", 100L, null, RecipientRole.GUARDIAN)));

        mockMvc.perform(post("/v1/sms/recipient-groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "sms-group-key-0001")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("레벨업학원 전체 학부모님"))
                .andExpect(jsonPath("$.recipients[0].phone").value("010-****-5678"));
    }

    @Test
    @DisplayName("POST /v1/sms/recipient-groups - Idempotency-Key 누락 시 400을 반환한다")
    void createRecipientGroup_missingIdempotencyKey_returns400() throws Exception {
        CreateSmsRecipientGroupRequest request = new CreateSmsRecipientGroupRequest(
                "레벨업학원 전체 학부모님",
                "mic",
                List.of(new RecipientRequest("010-1234-5678", "홍길동", 100L, null, RecipientRole.GUARDIAN)));

        mockMvc.perform(post("/v1/sms/recipient-groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /v1/sms/history - 발송 이력 조회 시 200을 반환한다")
    void listMessages_returns200() throws Exception {
        SmsMessageResponse response = buildSmsResponse(1L);
        Page<SmsMessageResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1);

        given(queryService.listMessages(any(), any(), any(), any(), any())).willReturn(page);

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
        given(queryService.getMessage(999L)).willThrow(new BusinessException(SmsErrorCode.MESSAGE_NOT_FOUND, "SMS message not found with id: 999"));

        mockMvc.perform(get("/v1/sms/history/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /v1/sms/send/class/{classId} - 반 문자 발송 시 201을 반환한다")
    void sendToClass_returns201() throws Exception {
        SmsMessageResponse response = buildSmsResponse(2L);
        given(commandService.sendToClass(any(Long.class), any(SendToClassRequest.class))).willReturn(response);
        given(idempotencyService.executeOnceWithoutResponseBody(anyString(), anyString(), any(), any()))
                .willAnswer(inv -> ((java.util.function.Supplier<?>) inv.getArgument(3)).get());

        SendToClassRequest request = new SendToClassRequest(RecipientRole.GUARDIAN, "제목", "내용", null);

        mockMvc.perform(post("/v1/sms/send/class/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "sms-class-key-0001")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    @DisplayName("POST /v1/sms/send/class/{classId} - 수신자 역할 누락 시 400을 반환한다")
    void sendToClass_missingRecipientRole_returns400() throws Exception {
        String request = """
                {"title": "제목", "content": "내용"}
                """;

        mockMvc.perform(post("/v1/sms/send/class/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "sms-class-key-0002")
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    private SmsMessageResponse buildSmsResponse(Long id) {
        return new SmsMessageResponse(
                id, 10L, RecipientType.GUARDIAN,
                List.of(new RecipientResponse("010-1234-5678", "홍길동", 100L, null, RecipientRole.GUARDIAN, null)),
                "테스트 제목", "테스트 내용", null,
                Instant.now(), SmsStatus.SENT, 1, 0,
                Instant.now(), Instant.now());
    }

    private SmsRecipientGroupResponse buildRecipientGroupResponse(Long id) {
        return new SmsRecipientGroupResponse(
                id,
                "레벨업학원 전체 학부모님",
                "mic",
                List.of(new RecipientResponse("010-****-5678", "홍길동", 100L, null, RecipientRole.GUARDIAN, null)),
                Instant.now(),
                Instant.now());
    }

    private SmsRecipientGroupSummaryResponse buildRecipientGroupSummaryResponse(Long id) {
        return new SmsRecipientGroupSummaryResponse(
                id,
                "레벨업학원 전체 학부모님",
                "mic",
                1,
                0,
                1,
                Instant.now(),
                Instant.now());
    }
}
