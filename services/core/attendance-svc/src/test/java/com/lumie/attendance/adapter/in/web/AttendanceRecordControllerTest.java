package com.lumie.attendance.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.attendance.application.dto.AttendanceRecordResponse;
import com.lumie.attendance.application.dto.BulkUpdateStatusRequest;
import com.lumie.attendance.application.dto.UpdateAttendanceStatusRequest;
import com.lumie.attendance.application.service.AttendanceRecordCommandService;
import com.lumie.attendance.application.service.AttendanceRecordQueryService;
import com.lumie.attendance.domain.vo.AttendanceStatus;
import com.lumie.common.exception.GlobalExceptionHandler;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttendanceRecordController 단위 테스트")
class AttendanceRecordControllerTest {

    @Mock
    private AttendanceRecordCommandService commandService;

    @Mock
    private AttendanceRecordQueryService queryService;

    @InjectMocks
    private AttendanceRecordController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    @DisplayName("GET /sessions/{sessionId}/records 호출 시 레코드 목록을 반환한다")
    void getRecords_returnsList() throws Exception {
        // given
        AttendanceRecordResponse record = new AttendanceRecordResponse(
            1L, 10L, 5L, "홍길동", AttendanceStatus.PRESENT, LocalDateTime.now(), null);
        given(queryService.getRecordsBySession(10L)).willReturn(List.of(record));

        // when / then
        mockMvc.perform(get("/v1/attendance/sessions/10/records"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].studentName").value("홍길동"))
            .andExpect(jsonPath("$[0].status").value("PRESENT"));
    }

    @Test
    @DisplayName("GET /sessions/{sessionId}/records 호출 시 레코드가 없으면 빈 목록을 반환한다")
    void getRecords_emptyList() throws Exception {
        // given
        given(queryService.getRecordsBySession(10L)).willReturn(List.of());

        // when / then
        mockMvc.perform(get("/v1/attendance/sessions/10/records"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("PATCH /sessions/{sessionId}/records/{recordId} 호출 시 204 No Content를 반환한다")
    void updateStatus_returns204() throws Exception {
        // given
        UpdateAttendanceStatusRequest request = new UpdateAttendanceStatusRequest(AttendanceStatus.EXCUSED, "병결");

        // when / then
        mockMvc.perform(patch("/v1/attendance/sessions/10/records/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());

        verify(commandService).updateStatus(eq(1L), any(UpdateAttendanceStatusRequest.class));
    }

    @Test
    @DisplayName("PATCH /sessions/{sessionId}/records/{recordId} 호출 시 status가 null이면 400을 반환한다")
    void updateStatus_nullStatus_returns400() throws Exception {
        mockMvc.perform(patch("/v1/attendance/sessions/10/records/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":null}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /sessions/{sessionId}/records/bulk-update 호출 시 204 No Content를 반환한다")
    void bulkUpdateStatus_returns204() throws Exception {
        // given
        BulkUpdateStatusRequest request = new BulkUpdateStatusRequest(List.of(1L, 2L), AttendanceStatus.ABSENT);

        // when / then
        mockMvc.perform(post("/v1/attendance/sessions/10/records/bulk-update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());

        verify(commandService).bulkUpdateStatus(eq(10L), any(BulkUpdateStatusRequest.class));
    }

    @Test
    @DisplayName("POST /sessions/{sessionId}/records/bulk-update 호출 시 recordIds가 비어 있으면 400을 반환한다")
    void bulkUpdateStatus_emptyRecordIds_returns400() throws Exception {
        mockMvc.perform(post("/v1/attendance/sessions/10/records/bulk-update")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recordIds\":[],\"status\":\"ABSENT\"}"))
            .andExpect(status().isBadRequest());
    }
}
