package com.lumie.staff.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lumie.common.auth.Role;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.CommonErrorCode;
import com.lumie.common.exception.GlobalExceptionHandler;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.staff.application.service.StaffCommandService;
import com.lumie.staff.application.service.StaffQueryService;
import com.lumie.staff.application.service.StaffStatisticsQueryService;
import com.lumie.staff.application.service.PermissionQueryService;
import com.lumie.staff.application.dto.request.StaffUpdateRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.lumie.staff.application.dto.response.StaffResponse;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

@ExtendWith(MockitoExtension.class)
class StaffRoleTest {

    @Mock private StaffCommandService commandService;
    @Mock private StaffQueryService queryService;
    @Mock private StaffStatisticsQueryService statisticsQueryService;
    @Mock private PermissionQueryService permissionQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new StaffController(commandService, queryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @AfterEach
    void clearUserContext() {
        UserContextHolder.clear();
    }

    @Test
    void instructorCanReadStaffListAndDetails() throws Exception {
        UserContextHolder.setUserRole(Role.INSTRUCTOR);
        given(queryService.getAllStaff(any(), any()))
                .willReturn(new PageImpl<>(java.util.List.of(), PageRequest.of(0, 20), 0));
        given(queryService.getStaff(42L)).willReturn(new StaffResponse(
                42L, 10L, "instructor_42", "Instructor", "", null, "INSTRUCTOR", null,
                true, null, null, null, null));

        mockMvc.perform(get("/v1/staff"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/v1/staff/42"))
                .andExpect(status().isOk());

        verify(queryService).getAllStaff(any(), any());
        verify(queryService).getStaff(42L);
    }

    @Test
    void studentCannotReadStaffAccounts() throws Exception {
        UserContextHolder.setUserRole(Role.STUDENT);

        mockMvc.perform(get("/v1/staff")).andExpect(status().isForbidden());
        mockMvc.perform(get("/v1/staff/42")).andExpect(status().isForbidden());

        verifyNoInteractions(queryService);
    }

    @Test
    void instructorIsDeniedAtEntryToStaffWritesAndPermissions() throws Exception {
        UserContextHolder.setUserRole(Role.INSTRUCTOR);
        StaffController controller = new StaffController(commandService, queryService);
        var handlers = Arrays.stream(StaffController.class.getDeclaredMethods())
                .filter(StaffRoleTest::isMappedHandler)
                .filter(handler -> !handler.getName().equals("getStaff")
                        && !handler.getName().equals("getAllStaff"))
                .toList();

        for (Method handler : handlers) {
            try {
                handler.invoke(controller, new Object[handler.getParameterCount()]);
                throw new AssertionError("Expected 403 at route entry for " + handler.getName());
            } catch (InvocationTargetException exception) {
                assertThat(exception.getCause())
                        .as("route handler %s", handler.getName())
                        .isInstanceOfSatisfying(com.lumie.common.exception.BusinessException.class, business ->
                                org.assertj.core.api.Assertions.assertThat(business.getErrorCode())
                                        .isEqualTo(com.lumie.common.exception.CommonErrorCode.FORBIDDEN));
            }
        }

        verifyNoInteractions(commandService, queryService);
    }

    @Test
    void managerCanUpdateButCannotDeleteStaff() {
        UserContextHolder.setUserRole(Role.MANAGER);
        StaffController controller = new StaffController(commandService, queryService);
        StaffUpdateRequest request = new StaffUpdateRequest("Updated", null, null, null);

        assertThat(controller.updateStaff(42L, request).getStatusCode().value()).isEqualTo(200);
        assertThatThrownBy(() -> controller.deleteStaff(42L))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(CommonErrorCode.FORBIDDEN));

        verify(commandService).updateStaff(42L, request);
        verify(commandService, never()).deleteStaff(42L);
    }

    @Test
    void ownerCanDeleteStaff() {
        UserContextHolder.setUserRole(Role.OWNER);
        StaffController controller = new StaffController(commandService, queryService);

        assertThat(controller.deleteStaff(42L).getStatusCode().value()).isEqualTo(204);

        verify(commandService).deleteStaff(42L);
    }

    @Test
    void instructorIsDeniedBeforeStaffStatisticsAndPermissionServices() throws Exception {
        UserContextHolder.setUserRole(Role.INSTRUCTOR);
        Object[][] controllers = {
            {new StaffStatisticsController(statisticsQueryService), statisticsQueryService},
            {new PermissionController(permissionQueryService), permissionQueryService}
        };

        for (Object[] entry : controllers) {
            Object controller = entry[0];
            Object service = entry[1];
            for (Method handler : Arrays.stream(controller.getClass().getDeclaredMethods())
                    .filter(StaffRoleTest::isMappedHandler)
                    .toList()) {
                try {
                    handler.invoke(controller, new Object[handler.getParameterCount()]);
                    throw new AssertionError("Expected 403 at route entry for " + handler.getName());
                } catch (InvocationTargetException exception) {
                    assertThat(exception.getCause())
                            .isInstanceOf(com.lumie.common.exception.BusinessException.class);
                }
            }
            verifyNoInteractions(service);
        }
    }

    @Test
    void ownerCanEnterEveryStaffAndPermissionHandler() throws Exception {
        UserContextHolder.setUserRole(Role.OWNER);
        org.mockito.BDDMockito.given(queryService.getAllStaff(
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .willReturn(new PageImpl<StaffResponse>(java.util.List.of(), PageRequest.of(0, 20), 0));
        Object[] controllers = {
                new StaffController(commandService, queryService),
                new StaffStatisticsController(statisticsQueryService),
                new PermissionController(permissionQueryService)
        };

        for (Object controller : controllers) {
            for (Method handler : Arrays.stream(controller.getClass().getDeclaredMethods())
                    .filter(StaffRoleTest::isMappedHandler)
                    .toList()) {
                Object result = handler.invoke(controller, defaultAllowedArguments(handler));
                assertThat(result)
                        .as("route handler %s", handler.getName())
                        .isInstanceOf(org.springframework.http.ResponseEntity.class);
            }
        }
    }

    private static boolean isMappedHandler(Method method) {
        return Arrays.stream(method.getDeclaredAnnotations())
                .anyMatch(annotation -> annotation.annotationType().getSimpleName().endsWith("Mapping"));
    }

    private static Object[] defaultAllowedArguments(Method method) {
        Class<?>[] types = method.getParameterTypes();
        Object[] arguments = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            if (Pageable.class.isAssignableFrom(types[i])) {
                arguments[i] = PageRequest.of(0, 20);
            } else if (types[i] == int.class) {
                arguments[i] = 0;
            } else if (types[i] == long.class) {
                arguments[i] = 0L;
            } else if (types[i] == boolean.class) {
                arguments[i] = false;
            } else if (types[i].getName().contains(".dto.request.")) {
                arguments[i] = org.mockito.Mockito.mock(types[i]);
            }
        }
        return arguments;
    }
}
