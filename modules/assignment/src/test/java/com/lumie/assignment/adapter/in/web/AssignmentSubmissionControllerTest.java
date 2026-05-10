package com.lumie.assignment.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lumie.assignment.application.dto.CreateSubmissionRequest;
import com.lumie.assignment.application.dto.SubmissionResponse;
import com.lumie.assignment.application.dto.UpdateSubmissionRequest;
import com.lumie.assignment.application.service.AssignmentCommandService;
import com.lumie.assignment.application.service.AssignmentQueryService;
import com.lumie.assignment.domain.exception.DuplicateSubmissionException;
import com.lumie.assignment.domain.exception.SubmissionNotFoundException;
import com.lumie.assignment.domain.vo.SubmissionStatus;
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
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssignmentSubmissionController 테스트")
class AssignmentSubmissionControllerTest {

    @Mock private AssignmentCommandService commandService;
    @Mock private AssignmentQueryService queryService;
    @InjectMocks private AssignmentSubmissionController controller;

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
    @DisplayName("GET /v1/assignments/{assignmentId}/submissions - 제출 목록 조회 시 200을 반환한다")
    void getSubmissions_returns200() throws Exception {
        SubmissionResponse submission = buildSubmissionResponse(10L);
        Page<SubmissionResponse> page = new PageImpl<>(List.of(submission), PageRequest.of(0, 20), 1);

        given(queryService.getSubmissions(eq(1L), any())).willReturn(page);

        mockMvc.perform(get("/v1/assignments/1/submissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].studentId").value(42));
    }

    @Test
    @DisplayName("POST /v1/assignments/{assignmentId}/submissions - 과제 제출 시 201을 반환한다")
    void createSubmission_returns201() throws Exception {
        SubmissionResponse response = buildSubmissionResponse(10L);
        given(commandService.createSubmission(eq(1L), any(CreateSubmissionRequest.class))).willReturn(response);

        CreateSubmissionRequest request = new CreateSubmissionRequest(42L, "답안 내용", null);

        mockMvc.perform(post("/v1/assignments/1/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.studentId").value(42));
    }

    @Test
    @DisplayName("POST /v1/assignments/{assignmentId}/submissions - 중복 제출 시 409를 반환한다")
    void createSubmission_duplicate_returns409() throws Exception {
        given(commandService.createSubmission(eq(1L), any(CreateSubmissionRequest.class)))
                .willThrow(new DuplicateSubmissionException(1L, 42L));

        CreateSubmissionRequest request = new CreateSubmissionRequest(42L, "답안", null);

        mockMvc.perform(post("/v1/assignments/1/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PATCH /v1/assignments/{assignmentId}/submissions/{submissionId} - 채점 시 200을 반환한다")
    void updateSubmission_returns200() throws Exception {
        SubmissionResponse response = new SubmissionResponse(
                10L, 1L, 42L, "답안", null, 85, "잘했습니다.",
                Instant.now(), Instant.now(), SubmissionStatus.GRADED,
                Instant.now(), Instant.now());
        given(commandService.updateSubmission(eq(1L), eq(10L), any(UpdateSubmissionRequest.class)))
                .willReturn(response);

        UpdateSubmissionRequest request = new UpdateSubmissionRequest(85, "잘했습니다.", "GRADED");

        mockMvc.perform(patch("/v1/assignments/1/submissions/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(85))
                .andExpect(jsonPath("$.status").value("GRADED"));
    }

    @Test
    @DisplayName("PATCH - 존재하지 않는 submission 수정 시 404를 반환한다")
    void updateSubmission_notFound_returns404() throws Exception {
        given(commandService.updateSubmission(eq(1L), eq(999L), any(UpdateSubmissionRequest.class)))
                .willThrow(new SubmissionNotFoundException(999L));

        UpdateSubmissionRequest request = new UpdateSubmissionRequest(null, null, null);

        mockMvc.perform(patch("/v1/assignments/1/submissions/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /v1/assignments/{assignmentId}/submissions/student/{studentId} - 학생별 제출 조회 시 200을 반환한다")
    void getStudentSubmissions_returns200() throws Exception {
        SubmissionResponse submission = buildSubmissionResponse(10L);
        Page<SubmissionResponse> page = new PageImpl<>(List.of(submission), PageRequest.of(0, 20), 1);

        given(queryService.getStudentSubmissions(eq(1L), eq(42L), any())).willReturn(page);

        mockMvc.perform(get("/v1/assignments/1/submissions/student/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10));
    }

    private SubmissionResponse buildSubmissionResponse(Long id) {
        return new SubmissionResponse(
                id, 1L, 42L, "답안 내용", null, null, null,
                Instant.now(), null, SubmissionStatus.SUBMITTED,
                Instant.now(), Instant.now());
    }
}
