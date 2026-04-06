package com.lumie.journal.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lumie.journal.application.dto.CreateJournalRequest;
import com.lumie.journal.application.dto.JournalResponse;
import com.lumie.journal.application.dto.UpdateJournalRequest;
import com.lumie.journal.application.service.JournalCommandService;
import com.lumie.journal.application.service.JournalQueryService;
import com.lumie.journal.domain.exception.JournalNotFoundException;
import com.lumie.journal.domain.vo.JournalStatus;
import com.lumie.journal.domain.vo.JournalVisibility;
import com.lumie.journal.domain.vo.ProcessingStatus;
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

import java.time.LocalDate;
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
@DisplayName("JournalController 테스트")
class JournalControllerTest {

    @Mock private JournalCommandService commandService;
    @Mock private JournalQueryService queryService;
    @InjectMocks private JournalController controller;

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
    @DisplayName("POST /v1/journals - 수업일지 생성 시 201을 반환한다")
    void createJournal_returns201() throws Exception {
        JournalResponse response = buildJournalResponse(1L);
        given(commandService.createJournal(any(CreateJournalRequest.class))).willReturn(response);

        CreateJournalRequest request = new CreateJournalRequest(
                1L, 1L, 10L, LocalDate.of(2026, 4, 1),
                "수학 수업일지", "미적분 기초", List.of("수학"), "숙제 없음",
                "TEACHER_ONLY", null);

        mockMvc.perform(post("/v1/journals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("수학 수업일지"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /v1/journals - 수업일지 목록 조회 시 200을 반환한다")
    void listJournals_returns200() throws Exception {
        JournalResponse response = buildJournalResponse(1L);
        Page<JournalResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1);

        given(queryService.listJournals(any(), any(), any(), any(), any(), any(), any(), any())).willReturn(page);

        mockMvc.perform(get("/v1/journals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("수학 수업일지"));
    }

    @Test
    @DisplayName("GET /v1/journals/{id} - 수업일지 단건 조회 시 200을 반환한다")
    void getJournal_returns200() throws Exception {
        JournalResponse response = buildJournalResponse(1L);
        given(queryService.getJournal(1L)).willReturn(response);

        mockMvc.perform(get("/v1/journals/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("수학 수업일지"));
    }

    @Test
    @DisplayName("GET /v1/journals/{id} - 존재하지 않는 수업일지 조회 시 404를 반환한다")
    void getJournal_notFound_returns404() throws Exception {
        given(queryService.getJournal(999L)).willThrow(new JournalNotFoundException(999L));

        mockMvc.perform(get("/v1/journals/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /v1/journals/{id} - 수업일지 수정 시 200을 반환한다")
    void updateJournal_returns200() throws Exception {
        JournalResponse response = buildJournalResponse(1L);
        given(commandService.updateJournal(eq(1L), any(UpdateJournalRequest.class))).willReturn(response);

        UpdateJournalRequest request = new UpdateJournalRequest(
                "변경된 제목", null, null, null, null, null, null, null);

        mockMvc.perform(patch("/v1/journals/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("DELETE /v1/journals/{id} - 수업일지 삭제 시 204를 반환한다")
    void deleteJournal_returns204() throws Exception {
        doNothing().when(commandService).deleteJournal(1L);

        mockMvc.perform(delete("/v1/journals/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /v1/journals/{id} - 존재하지 않는 수업일지 삭제 시 404를 반환한다")
    void deleteJournal_notFound_returns404() throws Exception {
        doThrow(new JournalNotFoundException(999L)).when(commandService).deleteJournal(999L);

        mockMvc.perform(delete("/v1/journals/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /v1/journals/class/{classId} - 수업별 수업일지 조회 시 200을 반환한다")
    void getJournalsByClass_returns200() throws Exception {
        JournalResponse response = buildJournalResponse(1L);
        Page<JournalResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1);

        given(queryService.getJournalsByClass(eq(1L), any())).willReturn(page);

        mockMvc.perform(get("/v1/journals/class/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].classId").value(1));
    }

    @Test
    @DisplayName("GET /v1/journals/teacher/{teacherId} - 강사별 수업일지 조회 시 200을 반환한다")
    void getJournalsByTeacher_returns200() throws Exception {
        JournalResponse response = buildJournalResponse(1L);
        Page<JournalResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1);

        given(queryService.getJournalsByTeacher(eq(10L), any())).willReturn(page);

        mockMvc.perform(get("/v1/journals/teacher/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].teacherId").value(10));
    }

    private JournalResponse buildJournalResponse(Long id) {
        return new JournalResponse(
                id, 1L, 1L, 10L,
                LocalDate.of(2026, 4, 1),
                "수학 수업일지", "미적분 기초", null, null,
                null, null, List.of("수학"), "숙제 없음",
                JournalVisibility.TEACHER_ONLY,
                ProcessingStatus.COMPLETED,
                JournalStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now());
    }
}
