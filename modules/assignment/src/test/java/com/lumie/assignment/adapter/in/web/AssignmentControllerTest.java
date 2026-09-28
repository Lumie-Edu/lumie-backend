package com.lumie.assignment.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lumie.assignment.application.dto.request.CreateAssignmentRequest;
import com.lumie.assignment.application.dto.request.UpdateAssignmentRequest;
import com.lumie.assignment.application.dto.response.AssignmentResponse;
import com.lumie.assignment.application.service.AssignmentCommandService;
import com.lumie.assignment.application.service.AssignmentQueryService;
import com.lumie.assignment.domain.exception.AssignmentErrorCode;
import com.lumie.common.exception.BusinessException;
import com.lumie.assignment.domain.vo.AssignmentStatus;
import com.lumie.common.auth.Role;
import com.lumie.common.exception.GlobalExceptionHandler;
import com.lumie.common.tenant.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
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
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssignmentController 테스트")
class AssignmentControllerTest {

    @Mock private AssignmentCommandService commandService;
    @Mock private AssignmentQueryService queryService;
    @InjectMocks private AssignmentController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        UserContextHolder.setUserRole(Role.INSTRUCTOR);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("POST /v1/assignments - 과제 생성 시 201을 반환한다")
    void createAssignment_returns201() throws Exception {
        AssignmentResponse response = buildAssignmentResponse(1L);
        given(commandService.createAssignment(any(CreateAssignmentRequest.class))).willReturn(response);

        CreateAssignmentRequest request = new CreateAssignmentRequest(
                1L, LocalDateTime.of(2026, 4, 15, 23, 59).toInstant(ZoneOffset.UTC), 100, null,
                null, "EXAM_MANUAL", "EXAM_AUTO",
                new com.lumie.assignment.application.dto.request.CreateAssignmentExamRequest(
                        "시험", "GRADED", "ABSOLUTE", "NINE_GRADE", 1,
                        java.util.Map.of("1", "2"), java.util.Map.of("1", 100), null, null),
                List.of(1L), List.of(), null, null, null, null, null);

        mockMvc.perform(post("/v1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.examName").value("수학 숙제"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /v1/assignments - 마감일이 없으면 400을 반환한다")
    void createAssignment_missingDueDate_returns400() throws Exception {
        CreateAssignmentRequest request = new CreateAssignmentRequest(
                1L, null, 100, null);

        mockMvc.perform(post("/v1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation error"));
    }

    @Test
    void createAssignment_existingExamWithoutNewExamIsRejected() throws Exception {
        mockMvc.perform(post("/v1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"classId":1,"dueDate":"2027-01-01T00:00:00Z","linkedExamId":99}
                                """))
                .andExpect(status().isBadRequest());
        org.mockito.Mockito.verifyNoInteractions(commandService);
    }

    @Test
    @DisplayName("GET /v1/assignments - 과제 목록 조회 시 200을 반환한다")
    void listAssignments_returns200() throws Exception {
        AssignmentResponse response = buildAssignmentResponse(1L);
        Page<AssignmentResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1);

        given(queryService.listAssignments(any(), any(), any())).willReturn(page);

        mockMvc.perform(get("/v1/assignments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].examName").value("수학 숙제"));
    }

    @Test
    @DisplayName("GET /v1/assignments/{id} - 과제 단건 조회 시 200을 반환한다")
    void getAssignment_returns200() throws Exception {
        AssignmentResponse response = buildAssignmentResponse(1L);
        given(queryService.getAssignment(1L)).willReturn(response);

        mockMvc.perform(get("/v1/assignments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.examName").value("수학 숙제"));
    }

    @Test
    @DisplayName("GET /v1/assignments/{id} - 존재하지 않는 과제 조회 시 404를 반환한다")
    void getAssignment_notFound_returns404() throws Exception {
        given(queryService.getAssignment(999L)).willThrow(new BusinessException(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND, "Assignment not found with id: 999"));

        mockMvc.perform(get("/v1/assignments/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /v1/assignments/{id} - 과제 수정 시 200을 반환한다")
    void updateAssignment_returns200() throws Exception {
        AssignmentResponse response = buildAssignmentResponse(1L);
        given(commandService.updateAssignment(eq(1L), any(UpdateAssignmentRequest.class))).willReturn(response);

        UpdateAssignmentRequest request = new UpdateAssignmentRequest(
                null, null, null, null);

        mockMvc.perform(patch("/v1/assignments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("DELETE /v1/assignments/{id} - 과제 삭제 시 204를 반환한다")
    void deleteAssignment_returns204() throws Exception {
        doNothing().when(commandService).deleteAssignment(1L);

        mockMvc.perform(delete("/v1/assignments/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /v1/assignments/{id} - 존재하지 않는 과제 삭제 시 404를 반환한다")
    void deleteAssignment_notFound_returns404() throws Exception {
        doThrow(new BusinessException(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND, "Assignment not found with id: 999")).when(commandService).deleteAssignment(999L);

        mockMvc.perform(delete("/v1/assignments/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /v1/assignments/class/{classId} - 수업별 과제 조회 시 200을 반환한다")
    void getAssignmentsByClass_returns200() throws Exception {
        AssignmentResponse response = buildAssignmentResponse(1L);
        Page<AssignmentResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1);

        given(queryService.getAssignmentsByClass(eq(1L), any())).willReturn(page);

        mockMvc.perform(get("/v1/assignments/class/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(1));
    }

    private AssignmentResponse buildAssignmentResponse(Long id) {
        return new AssignmentResponse(
                id, 1L, LocalDateTime.of(2026, 4, 15, 23, 59).toInstant(ZoneOffset.UTC),
                100, null, AssignmentStatus.ACTIVE,
                com.lumie.assignment.domain.vo.SubmissionMode.EXAM_MANUAL,
                com.lumie.assignment.domain.vo.EvaluationMode.EXAM_AUTO,
                10L, "수학 숙제", List.of(1L), List.of(),
                true, true, false, true, false,
                Instant.now(), Instant.now());
    }
}
