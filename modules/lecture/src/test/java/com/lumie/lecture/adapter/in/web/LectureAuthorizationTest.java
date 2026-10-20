package com.lumie.lecture.adapter.in.web;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;

import com.lumie.common.auth.Role;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.lecture.application.service.LectureCommandService;
import com.lumie.lecture.application.service.LectureQueryService;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class LectureAuthorizationTest {

    @Mock private LectureCommandService commandService;
    @Mock private LectureQueryService queryService;

    @AfterEach
    void clearUserContext() {
        UserContextHolder.clear();
    }

    @Test
    void studentIsDeniedAtEntryToEveryStaffLectureHandler() throws Exception {
        UserContextHolder.setUserRole(Role.STUDENT);
        LectureController controller = new LectureController(commandService, queryService);

        for (Method handler : Arrays.stream(LectureController.class.getDeclaredMethods())
                .filter(LectureAuthorizationTest::isMappedHandler)
                .toList()) {
            try {
                handler.invoke(controller, new Object[handler.getParameterCount()]);
                throw new AssertionError("Expected 403 at route entry for " + handler.getName());
            } catch (InvocationTargetException exception) {
                org.assertj.core.api.Assertions.assertThat(exception.getCause())
                        .isInstanceOf(BusinessException.class);
            }
        }

        verifyNoInteractions(commandService, queryService);
    }

    @Test
    void instructorCanEnterEveryStaffLectureHandler() throws Exception {
        UserContextHolder.setUserRole(Role.INSTRUCTOR);
        given(queryService.findAll(
                        nullable(Long.class), nullable(Long.class), nullable(LocalDate.class), nullable(LocalDate.class),
                        nullable(String.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(java.util.List.of(), PageRequest.of(0, 20), 0));
        LectureController controller = new LectureController(commandService, queryService);

        for (Method handler : Arrays.stream(LectureController.class.getDeclaredMethods())
                .filter(LectureAuthorizationTest::isMappedHandler)
                .toList()) {
            Object result = handler.invoke(controller, allowedArguments(handler));
            org.assertj.core.api.Assertions.assertThat(result)
                    .as("route handler %s", handler.getName())
                    .isInstanceOf(org.springframework.http.ResponseEntity.class);
        }
    }

    private static boolean isMappedHandler(Method method) {
        return Arrays.stream(method.getDeclaredAnnotations())
                .anyMatch(annotation -> annotation.annotationType().getSimpleName().endsWith("Mapping"));
    }

    private static Object[] allowedArguments(Method method) {
        Class<?>[] types = method.getParameterTypes();
        Object[] args = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            if (Pageable.class.isAssignableFrom(types[i])) args[i] = PageRequest.of(0, 20);
            else if (types[i] == Long.class) args[i] = 1L;
            else if (types[i] == String.class) args[i] = "search";
        }
        return args;
    }
}
