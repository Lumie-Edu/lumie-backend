package com.lumie.common.activitylog;

import com.lumie.activitylog.api.ActivityLogService;
import com.lumie.common.auth.ActionType;
import com.lumie.common.auth.EntityType;
import com.lumie.common.auth.Role;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link LogActivityAspect}.
 *
 * <p>Uses {@link AspectJProxyFactory} to create a real AOP proxy around a
 * lightweight stub service — no Spring context needed, no Postgres.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LogActivityAspect")
class LogActivityAspectTest {

    @Mock
    private ActivityLogService activityLogService;

    private StubService proxy;

    @BeforeEach
    void setUp() {
        LogActivityAspect aspect = new LogActivityAspect(activityLogService);
        AspectJProxyFactory factory = new AspectJProxyFactory(new StubService());
        factory.addAspect(aspect);
        proxy = factory.getProxy();

        TenantContextHolder.setTenant("test-tenant");
        TenantContextHolder.setTenantId(1L);
        UserContextHolder.setUserId(42L);
        UserContextHolder.setUserName("김철수");
        UserContextHolder.setUserRole(Role.MANAGER);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        UserContextHolder.clear();
        ActivityLogContext.clear();
    }

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("logs activity and marks context when tenant + user are set")
        void logs_and_marksLogged() {
            proxy.voidMethodWithIdArg(99L);

            verify(activityLogService).log(
                    eq(42L), eq("김철수"), eq("MANAGER"),
                    eq("DELETE"), eq("ADMIN"),
                    eq(99L), isNull(), isNull()
            );
            assertThat(ActivityLogContext.isLogged()).isTrue();
        }

        @Test
        @DisplayName("resolves entity id from return value getId() when entityIdArg is blank")
        void resolvesEntityIdFromReturnValue() {
            proxy.methodReturningGetId();

            verify(activityLogService).log(
                    eq(42L), anyString(), anyString(),
                    eq("CREATE"), eq("CLASS"),
                    eq(77L), isNull(), isNull()
            );
        }

        @Test
        @DisplayName("passes null entity id when return value has no getId() and entityIdArg is blank")
        void nullEntityIdWhenUnresolvable() {
            proxy.voidMethodNoIdArg();

            verify(activityLogService).log(
                    eq(42L), anyString(), anyString(),
                    eq("UPDATE"), eq("STUDENT"),
                    isNull(), isNull(), isNull()
            );
        }

        @Test
        @DisplayName("passes static entityName from annotation")
        void staticEntityName() {
            proxy.methodWithStaticEntityName();

            verify(activityLogService).log(
                    anyLong(), anyString(), anyString(),
                    anyString(), anyString(),
                    isNull(), eq("academy-main"), isNull()
            );
        }
    }

    @Nested
    @DisplayName("guard conditions — no log when context missing")
    class GuardConditions {

        @Test
        @DisplayName("skips log when tenant context is absent")
        void skipsWhenNoTenant() {
            TenantContextHolder.clear();

            proxy.voidMethodWithIdArg(1L);

            verify(activityLogService, never()).log(any(), any(), any(), any(), any(), any(), any(), any());
            assertThat(ActivityLogContext.isLogged()).isFalse();
        }

        @Test
        @DisplayName("skips log when user context is absent")
        void skipsWhenNoUser() {
            UserContextHolder.clear();

            proxy.voidMethodWithIdArg(1L);

            verify(activityLogService, never()).log(any(), any(), any(), any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("failure isolation")
    class FailureIsolation {

        @Test
        @DisplayName("swallows ActivityLogService exception — does not rethrow")
        void swallowsLoggingException() {
            org.mockito.Mockito.doThrow(new RuntimeException("db down"))
                    .when(activityLogService).log(any(), any(), any(), any(), any(), any(), any(), any());

            // must not throw
            proxy.voidMethodWithIdArg(1L);
        }
    }

    // ---- stub target service ----

    static class StubService {

        @LogActivity(action = ActionType.DELETE, entity = EntityType.ADMIN, entityIdArg = "adminId")
        public void voidMethodWithIdArg(Long adminId) {
        }

        @LogActivity(action = ActionType.UPDATE, entity = EntityType.STUDENT)
        public void voidMethodNoIdArg() {
        }

        @LogActivity(action = ActionType.CREATE, entity = EntityType.CLASS)
        public EntityWithId methodReturningGetId() {
            return new EntityWithId(77L);
        }

        @LogActivity(action = ActionType.CREATE, entity = EntityType.ACADEMY, entityName = "academy-main")
        public void methodWithStaticEntityName() {
        }
    }

    record EntityWithId(Long id) {
        public Long getId() {
            return id;
        }
    }
}
