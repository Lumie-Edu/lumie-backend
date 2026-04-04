package com.lumie.attendance.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.attendance.application.dto.CheckInRequest;
import com.lumie.attendance.application.dto.CheckInResponse;
import com.lumie.attendance.application.service.AttendanceRecordCommandService;
import com.lumie.attendance.domain.exception.InvalidCodeException;
import com.lumie.attendance.domain.exception.SessionClosedException;
import com.lumie.attendance.domain.vo.AttendanceStatus;
import com.lumie.attendance.infrastructure.exception.GlobalExceptionHandler;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudentCheckInController 단위 테스트")
class StudentCheckInControllerTest {

    @Mock
    private AttendanceRecordCommandService commandService;

    @InjectMocks
    private StudentCheckInController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    @DisplayName("유효한 코드로 체크인 시 200 OK와 응답을 반환한다")
    void checkIn_validCode_returns200() throws Exception {
        // given
        CheckInResponse response = new CheckInResponse("출석이 완료되었습니다.", AttendanceStatus.PRESENT);
        given(commandService.checkInByCode(eq(1L), any(CheckInRequest.class))).willReturn(response);

        // when / then
        mockMvc.perform(post("/v1/attendance/check-in")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CheckInRequest("123456"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PRESENT"))
            .andExpect(jsonPath("$.message").value("출석이 완료되었습니다."));
    }

    @Test
    @DisplayName("지각 체크인 시 200 OK와 LATE 상태를 반환한다")
    void checkIn_late_returns200WithLateStatus() throws Exception {
        // given
        CheckInResponse response = new CheckInResponse("지각으로 처리되었습니다.", AttendanceStatus.LATE);
        given(commandService.checkInByCode(eq(1L), any(CheckInRequest.class))).willReturn(response);

        // when / then
        mockMvc.perform(post("/v1/attendance/check-in")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CheckInRequest("654321"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("LATE"));
    }

    @Test
    @DisplayName("코드가 6자리 숫자가 아니면 400 Bad Request를 반환한다")
    void checkIn_invalidCodeFormat_returns400() throws Exception {
        mockMvc.perform(post("/v1/attendance/check-in")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CheckInRequest("abc"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("코드가 null이면 400 Bad Request를 반환한다")
    void checkIn_nullCode_returns400() throws Exception {
        mockMvc.perform(post("/v1/attendance/check-in")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":null}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("잘못된 코드로 체크인 시 400 Bad Request를 반환한다")
    void checkIn_codeNotFound_returns400() throws Exception {
        // given
        given(commandService.checkInByCode(any(), any())).willThrow(new InvalidCodeException());

        // when / then
        mockMvc.perform(post("/v1/attendance/check-in")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CheckInRequest("999999"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("닫힌 세션으로 체크인 시 400 Bad Request를 반환한다")
    void checkIn_closedSession_returns400() throws Exception {
        // given
        given(commandService.checkInByCode(any(), any())).willThrow(new SessionClosedException(1L));

        // when / then
        mockMvc.perform(post("/v1/attendance/check-in")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CheckInRequest("123456"))))
            .andExpect(status().isBadRequest());
    }
}
