package com.lumie.billing.adapter.in.web;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lumie.billing.application.service.BillingKeyCommandService;
import com.lumie.billing.application.service.BillingKeyQueryService;
import com.lumie.billing.application.service.BillingSubscribeService;
import com.lumie.billing.application.service.PaymentService;
import com.lumie.billing.application.service.SubscriptionCommandService;
import com.lumie.billing.application.service.SubscriptionQueryService;
import com.lumie.billing.application.service.AlimtalkCreditCommandService;
import com.lumie.billing.application.service.AlimtalkCreditQueryService;
import com.lumie.common.auth.Role;
import com.lumie.common.exception.GlobalExceptionHandler;
import com.lumie.common.idempotency.IdempotencyService;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.common.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BillingRoleTest {

    @Mock private BillingKeyCommandService commandService;
    @Mock private BillingKeyQueryService queryService;
    @Mock private IdempotencyService idempotencyService;
    @Mock private BillingSubscribeService subscribeService;
    @Mock private PaymentService paymentService;
    @Mock private SubscriptionCommandService subscriptionCommandService;
    @Mock private SubscriptionQueryService subscriptionQueryService;
    @Mock private AlimtalkCreditCommandService creditCommandService;
    @Mock private AlimtalkCreditQueryService creditQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new BillingKeyController(commandService, queryService, idempotencyService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void clearUserContext() {
        UserContextHolder.clear();
        TenantContextHolder.clear();
    }

    @Test
    void managerCannotReadBillingKeysBeforeQueryService() throws Exception {
        UserContextHolder.setUserRole(Role.MANAGER);

        mockMvc.perform(get("/v1/billing-keys"))
                .andExpect(status().isForbidden());

        verify(queryService, never()).getAllBillingKeys(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void instructorCannotReadBillingKeysBeforeQueryService() throws Exception {
        UserContextHolder.setUserRole(Role.INSTRUCTOR);

        mockMvc.perform(get("/v1/billing-keys"))
                .andExpect(status().isForbidden());

        verify(queryService, never()).getAllBillingKeys(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void studentCannotReadBillingKeysBeforeQueryService() throws Exception {
        UserContextHolder.setUserRole(Role.STUDENT);

        mockMvc.perform(get("/v1/billing-keys"))
                .andExpect(status().isForbidden());

        verify(queryService, never()).getAllBillingKeys(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void nonOwnerIsDeniedAtEntryToEveryBillingOperation() throws Exception {
        UserContextHolder.setUserRole(Role.MANAGER);
        Object[] controllers = {
                new BillingKeyController(commandService, queryService, idempotencyService),
                new BillingSubscribeController(subscribeService, idempotencyService),
                new PaymentController(paymentService, idempotencyService),
                new SubscriptionController(subscriptionCommandService, subscriptionQueryService, idempotencyService),
                new AlimtalkCreditController(creditCommandService, creditQueryService, idempotencyService)
        };

        for (Object controller : controllers) {
            for (Method handler : Arrays.stream(controller.getClass().getDeclaredMethods())
                    .filter(BillingRoleTest::isMappedHandler)
                    .toList()) {
                try {
                    handler.invoke(controller, defaultArguments(handler));
                    throw new AssertionError("Expected 403 at route entry for " + handler.getName());
                } catch (InvocationTargetException exception) {
                    org.assertj.core.api.Assertions.assertThat(exception.getCause())
                            .isInstanceOf(com.lumie.common.exception.BusinessException.class);
                }
            }
        }

        org.mockito.Mockito.verifyNoInteractions(commandService, queryService, subscribeService, paymentService,
                subscriptionCommandService, subscriptionQueryService, creditCommandService, creditQueryService,
                idempotencyService);
    }

    @Test
    void ownerCanEnterEveryBillingOperation() throws Exception {
        UserContextHolder.setUserRole(Role.OWNER);
        TenantContextHolder.setTenantId(10L);
        given(idempotencyService.executeOnce(nullable(String.class), anyString(), nullable(Object.class), any()))
                .willAnswer(invocation -> ((Supplier<?>) invocation.getArgument(3)).get());
        given(idempotencyService.executeOnceWithoutResponseBody(
                        nullable(String.class), anyString(), nullable(Object.class), any()))
                .willAnswer(invocation -> ((Supplier<?>) invocation.getArgument(3)).get());
        Object[] controllers = {
                new BillingKeyController(commandService, queryService, idempotencyService),
                new BillingSubscribeController(subscribeService, idempotencyService),
                new PaymentController(paymentService, idempotencyService),
                new SubscriptionController(subscriptionCommandService, subscriptionQueryService, idempotencyService),
                new AlimtalkCreditController(creditCommandService, creditQueryService, idempotencyService)
        };

        for (Object controller : controllers) {
            for (Method handler : Arrays.stream(controller.getClass().getDeclaredMethods())
                    .filter(BillingRoleTest::isMappedHandler)
                    .toList()) {
                Object result = handler.invoke(controller, defaultAllowedArguments(handler));
                org.assertj.core.api.Assertions.assertThat(result)
                        .as("route handler %s", handler.getName())
                        .isInstanceOf(org.springframework.http.ResponseEntity.class);
            }
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
            if (types[i].getName().contains(".dto.request.")) arguments[i] = org.mockito.Mockito.mock(types[i]);
            if (types[i] == long.class) arguments[i] = 0L;
            if (types[i] == boolean.class) arguments[i] = false;
        }
        return arguments;
    }

    private static Object[] defaultAllowedArguments(Method method) {
        Class<?>[] types = method.getParameterTypes();
        Object[] arguments = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            if (types[i] == String.class) arguments[i] = "tenant";
            if (types[i] == Long.class) arguments[i] = 10L;
            if (types[i] == long.class) arguments[i] = 0L;
            if (types[i] == boolean.class) arguments[i] = false;
            if (types[i] == int.class) arguments[i] = 0;
            if (Pageable.class.isAssignableFrom(types[i])) arguments[i] = PageRequest.of(0, 20);
            if (types[i].isEnum()) arguments[i] = types[i].getEnumConstants()[0];
            if (types[i].getName().contains(".dto.request.")) arguments[i] = org.mockito.Mockito.mock(types[i]);
        }
        return arguments;
    }
}
