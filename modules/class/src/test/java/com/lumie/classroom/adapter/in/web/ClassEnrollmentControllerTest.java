package com.lumie.classroom.adapter.in.web;

import com.lumie.common.auth.Role;
import com.lumie.common.tenant.UserContextHolder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lumie.classroom.application.dto.ClassEnrollmentResponse;
import com.lumie.classroom.application.dto.EnrollStudentsRequest;
import com.lumie.classroom.application.dto.UpdateEnrollmentRequest;
import com.lumie.classroom.application.service.ClassCommandService;
import com.lumie.classroom.application.service.ClassQueryService;
import com.lumie.classroom.domain.exception.ClassErrorCode;
import com.lumie.common.exception.BusinessException;
import com.lumie.classroom.domain.vo.EnrollmentStatus;
import com.lumie.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClassEnrollmentController 테스트")
class ClassEnrollmentControllerTest {

    @Mock private ClassCommandService commandService;
    @Mock private ClassQueryService queryService;
    @InjectMocks private ClassEnrollmentController controller;

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

    @Test
    void studentIsDeniedAtEntryToEveryEnrollmentHandler() throws Exception {
        UserContextHolder.setUserRole(Role.STUDENT);

        for (Method handler : ClassEnrollmentController.class.getDeclaredMethods()) {
            if (Arrays.stream(handler.getDeclaredAnnotations())
                    .noneMatch(annotation -> annotation.annotationType().getSimpleName().endsWith("Mapping"))) {
                continue;
            }
            try {
                handler.invoke(controller, new Object[handler.getParameterCount()]);
                throw new AssertionError("Expected 403 at route entry for " + handler.getName());
            } catch (InvocationTargetException exception) {
                org.assertj.core.api.Assertions.assertThat(exception.getCause())
                        .isInstanceOf(BusinessException.class);
            }
        }

        org.mockito.Mockito.verifyNoInteractions(commandService, queryService);
    }

    @Test
    void instructorCanEnterEveryEnrollmentHandler() throws Exception {
        UserContextHolder.setUserRole(Role.INSTRUCTOR);
        given(queryService.getEnrollments(eq(1L), any()))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        for (Method handler : ClassEnrollmentController.class.getDeclaredMethods()) {
            if (Arrays.stream(handler.getDeclaredAnnotations())
                    .noneMatch(annotation -> annotation.annotationType().getSimpleName().endsWith("Mapping"))) {
                continue;
            }
            Object result = handler.invoke(controller, allowedArguments(handler));
            org.assertj.core.api.Assertions.assertThat(result)
                    .as("route handler %s", handler.getName())
                    .isInstanceOf(org.springframework.http.ResponseEntity.class);
        }
    }

    private static Object[] allowedArguments(Method method) {
        Object[] args = new Object[method.getParameterCount()];
        Class<?>[] types = method.getParameterTypes();
        for (int i = 0; i < types.length; i++) {
            if (Pageable.class.isAssignableFrom(types[i])) {
                args[i] = PageRequest.of(0, 20);
            } else if (types[i] == Long.class) {
                args[i] = 1L;
            }
        }
        return args;
    }

    @AfterEach
    void clearUserContext() {
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("GET /v1/classes/{classId}/enrollments - 수강생 목록 조회 시 200을 반환한다")
    void getEnrollments_returns200() throws Exception {
        ClassEnrollmentResponse enrollment = buildEnrollmentResponse(10L);
        Page<ClassEnrollmentResponse> page = new PageImpl<>(List.of(enrollment), PageRequest.of(0, 20), 1);

        given(queryService.getEnrollments(eq(1L), any())).willReturn(page);

        mockMvc.perform(get("/v1/classes/1/enrollments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(10))
                .andExpect(jsonPath("$.items[0].studentId").value(42));
    }

    @Test
    @DisplayName("POST /v1/classes/{classId}/enrollments - 수강생 등록 시 201을 반환한다")
    void enrollStudents_returns201() throws Exception {
        List<ClassEnrollmentResponse> responses = List.of(
                buildEnrollmentResponse(10L), buildEnrollmentResponse(11L));
        given(commandService.enrollStudents(eq(1L), any(EnrollStudentsRequest.class))).willReturn(responses);

        EnrollStudentsRequest request = new EnrollStudentsRequest(List.of(42L, 43L));

        mockMvc.perform(post("/v1/classes/1/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[1].id").value(11));
    }

    @Test
    @DisplayName("DELETE /v1/classes/{classId}/enrollments/{enrollmentId} - 수강 취소 시 204를 반환한다")
    void removeEnrollment_returns204() throws Exception {
        doNothing().when(commandService).removeEnrollment(1L, 10L);

        mockMvc.perform(delete("/v1/classes/1/enrollments/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE - 존재하지 않는 enrollment 제거 시 404를 반환한다")
    void removeEnrollment_notFound_returns404() throws Exception {
        doThrow(new BusinessException(ClassErrorCode.ENROLLMENT_NOT_FOUND, "Enrollment not found with id: 999")).when(commandService).removeEnrollment(1L, 999L);

        mockMvc.perform(delete("/v1/classes/1/enrollments/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /v1/classes/{classId}/enrollments/{enrollmentId} - 상태 변경 시 200을 반환한다")
    void updateEnrollmentStatus_returns200() throws Exception {
        ClassEnrollmentResponse response = new ClassEnrollmentResponse(
                10L, 1L, 42L, Instant.now(), Instant.now(),
                EnrollmentStatus.DROPPED, Instant.now(), Instant.now());
        given(commandService.updateEnrollmentStatus(eq(1L), eq(10L), any(UpdateEnrollmentRequest.class)))
                .willReturn(response);

        UpdateEnrollmentRequest request = new UpdateEnrollmentRequest("DROPPED");

        mockMvc.perform(patch("/v1/classes/1/enrollments/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DROPPED"));
    }

    private ClassEnrollmentResponse buildEnrollmentResponse(Long id) {
        return new ClassEnrollmentResponse(
                id, 1L, 42L, Instant.now(), null,
                EnrollmentStatus.ENROLLED, Instant.now(), Instant.now());
    }
}
