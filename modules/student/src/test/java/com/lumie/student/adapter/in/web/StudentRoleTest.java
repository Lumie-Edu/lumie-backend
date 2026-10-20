package com.lumie.student.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lumie.common.auth.Role;
import com.lumie.common.exception.GlobalExceptionHandler;
import com.lumie.common.idempotency.IdempotencyService;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.student.application.dto.response.StudentResponse;
import com.lumie.student.application.service.StudentCommandService;
import com.lumie.student.application.service.StudentQueryService;
import java.util.List;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class StudentRoleTest {

    @Mock private StudentCommandService commandService;
    @Mock private StudentQueryService queryService;
    @Mock private IdempotencyService idempotencyService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new StudentController(commandService, queryService, idempotencyService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @AfterEach
    void clearUserContext() {
        UserContextHolder.clear();
        TenantContextHolder.clear();
    }

    @Test
    void studentCannotListOtherStudentsBeforeQueryService() throws Exception {
        UserContextHolder.setUserRole(Role.STUDENT);

        mockMvc.perform(get("/v1/students"))
                .andExpect(status().isForbidden());

        verify(queryService, never()).searchStudents(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void instructorCanListStudents() throws Exception {
        UserContextHolder.setUserRole(Role.INSTRUCTOR);
        org.mockito.BDDMockito.given(queryService.searchStudents(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .willReturn(new PageImpl<StudentResponse>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/v1/students"))
                .andExpect(status().isOk());

        verify(queryService).searchStudents(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void studentIsDeniedAtEntryToEveryStaffStudentHandler() throws Exception {
        UserContextHolder.setUserRole(Role.STUDENT);
        StudentController controller = new StudentController(commandService, queryService, idempotencyService);
        List<Method> handlers = Arrays.stream(StudentController.class.getDeclaredMethods())
                .filter(StudentRoleTest::isMappedHandler)
                .toList();

        assertThat(handlers).hasSizeGreaterThan(10);
        for (Method handler : handlers) {
            Object[] arguments = defaultArguments(handler);
            try {
                handler.invoke(controller, arguments);
                throw new AssertionError("Expected 403 at route entry for " + handler.getName());
            } catch (InvocationTargetException exception) {
                assertThat(exception.getCause())
                        .as("route handler %s", handler.getName())
                        .isInstanceOfSatisfying(com.lumie.common.exception.BusinessException.class, business ->
                                assertThat(business.getErrorCode()).isEqualTo(
                                        com.lumie.common.exception.CommonErrorCode.FORBIDDEN));
            }
        }

        verifyNoInteractions(commandService, queryService, idempotencyService);
    }

    @Test
    void ownerCanEnterEveryStudentManagementHandler() throws Exception {
        UserContextHolder.setUserRole(Role.OWNER);
        TenantContextHolder.setTenant("test-tenant");
        org.mockito.BDDMockito.given(queryService.searchStudents(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .willReturn(new PageImpl<StudentResponse>(List.of(), PageRequest.of(0, 20), 0));
        org.mockito.BDDMockito.given(idempotencyService.executeOnce(
                        org.mockito.ArgumentMatchers.nullable(String.class),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .willAnswer(invocation -> ((Supplier<?>) invocation.getArgument(3)).get());
        StudentController controller = new StudentController(commandService, queryService, idempotencyService);

        for (Method handler : Arrays.stream(StudentController.class.getDeclaredMethods())
                .filter(StudentRoleTest::isMappedHandler)
                .toList()) {
            Object result = handler.invoke(controller, defaultAllowedArguments(handler));
            assertThat(result)
                    .as("route handler %s", handler.getName())
                    .isInstanceOf(org.springframework.http.ResponseEntity.class);
        }
    }

    private static boolean isMappedHandler(Method method) {
        return Arrays.stream(method.getDeclaredAnnotations())
                .anyMatch(annotation -> annotation.annotationType().getSimpleName().endsWith("Mapping"));
    }

    private static Object[] defaultArguments(Method method) {
        Class<?>[] types = method.getParameterTypes();
        Object[] arguments = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            if (types[i] == int.class) arguments[i] = 0;
            if (types[i] == long.class) arguments[i] = 0L;
            if (types[i] == boolean.class) arguments[i] = false;
        }
        return arguments;
    }

    private static Object[] defaultAllowedArguments(Method method) {
        Object[] arguments = defaultArguments(method);
        Class<?>[] types = method.getParameterTypes();
        for (int i = 0; i < types.length; i++) {
            if (org.springframework.data.domain.Pageable.class.isAssignableFrom(types[i])) {
                arguments[i] = PageRequest.of(0, 20);
            } else if (org.springframework.web.multipart.MultipartFile.class.isAssignableFrom(types[i])) {
                arguments[i] = new org.springframework.mock.web.MockMultipartFile(
                        "file", "students.csv", "text/csv", new byte[0]);
            } else if (types[i] == String.class) {
                arguments[i] = "idempotency-key";
            } else if (types[i].getName().contains(".dto.request.")) {
                arguments[i] = org.mockito.Mockito.mock(types[i]);
            }
        }
        return arguments;
    }
}
