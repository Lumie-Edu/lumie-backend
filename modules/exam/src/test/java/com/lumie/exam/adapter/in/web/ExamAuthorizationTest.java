package com.lumie.exam.adapter.in.web;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lumie.common.auth.Role;
import com.lumie.common.exception.GlobalExceptionHandler;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.exam.application.service.ExamCommandService;
import com.lumie.exam.application.service.ExamQueryService;
import com.lumie.exam.application.service.ExamTemplateCommandService;
import com.lumie.exam.application.service.ExamTemplateQueryService;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ExamAuthorizationTest {

    @Mock private ExamCommandService examCommandService;
    @Mock private ExamQueryService examQueryService;
    @Mock private ExamTemplateCommandService templateCommandService;
    @Mock private ExamTemplateQueryService templateQueryService;

    private MockMvc examMvc;
    private MockMvc templateMvc;

    @BeforeEach
    void setUp() {
        examMvc = MockMvcBuilders.standaloneSetup(new ExamController(examCommandService, examQueryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        templateMvc = MockMvcBuilders.standaloneSetup(
                        new ExamTemplateController(templateCommandService, templateQueryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @AfterEach
    void clearUserContext() {
        UserContextHolder.clear();
    }

    @Test
    void studentCannotListExamsOrTemplatesBeforeQueryServices() throws Exception {
        UserContextHolder.setUserRole(Role.STUDENT);

        examMvc.perform(get("/v1/exams"))
                .andExpect(status().isForbidden());
        templateMvc.perform(get("/v1/exam-templates"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(examCommandService, examQueryService, templateCommandService, templateQueryService);
    }

    @Test
    void studentIsDeniedAtEntryToEveryExamAndTemplateHandler() throws Exception {
        UserContextHolder.setUserRole(Role.STUDENT);
        Object[] controllers = {
                new ExamController(examCommandService, examQueryService),
                new ExamTemplateController(templateCommandService, templateQueryService)
        };

        for (Object controller : controllers) {
            for (Method handler : Arrays.stream(controller.getClass().getDeclaredMethods())
                    .filter(ExamAuthorizationTest::isMappedHandler)
                    .toList()) {
                try {
                    handler.invoke(controller, new Object[handler.getParameterCount()]);
                    throw new AssertionError("Expected 403 at route entry for " + handler.getName());
                } catch (InvocationTargetException exception) {
                    org.assertj.core.api.Assertions.assertThat(exception.getCause())
                            .isInstanceOf(com.lumie.common.exception.BusinessException.class);
                }
            }
        }

        verifyNoInteractions(examCommandService, examQueryService, templateCommandService, templateQueryService);
    }

    @Test
    void instructorCanEnterEveryExamAndTemplateHandler() throws Exception {
        UserContextHolder.setUserRole(Role.INSTRUCTOR);
        given(examQueryService.listExams(org.mockito.ArgumentMatchers.any(Pageable.class)))
                .willReturn(new PageImpl<>(java.util.List.of(), PageRequest.of(0, 20), 0));
        given(templateQueryService.listTemplates(org.mockito.ArgumentMatchers.any(Pageable.class)))
                .willReturn(new PageImpl<>(java.util.List.of(), PageRequest.of(0, 20), 0));
        Object[] controllers = {
                new ExamController(examCommandService, examQueryService),
                new ExamTemplateController(templateCommandService, templateQueryService)
        };

        for (Object controller : controllers) {
            for (Method handler : Arrays.stream(controller.getClass().getDeclaredMethods())
                    .filter(ExamAuthorizationTest::isMappedHandler)
                    .toList()) {
                Object result = handler.invoke(controller, allowedArguments(handler));
                org.assertj.core.api.Assertions.assertThat(result)
                        .as("route handler %s", handler.getName())
                        .isInstanceOf(org.springframework.http.ResponseEntity.class);
            }
        }
    }

    private static Object[] allowedArguments(Method method) {
        Class<?>[] types = method.getParameterTypes();
        Object[] args = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            if (Pageable.class.isAssignableFrom(types[i])) args[i] = PageRequest.of(0, 20);
            else if (types[i] == Long.class) args[i] = 1L;
        }
        return args;
    }

    private static boolean isMappedHandler(Method method) {
        return Arrays.stream(method.getDeclaredAnnotations())
                .anyMatch(annotation -> annotation.annotationType().getSimpleName().endsWith("Mapping"));
    }
}
