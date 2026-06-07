package com.lumie.activitylog.application.service;

import com.lumie.activitylog.domain.entity.ActivityLog;
import com.lumie.activitylog.domain.repository.ActivityLogRepository;
import com.lumie.common.auth.ActionType;
import com.lumie.common.auth.EntityType;
import com.lumie.common.auth.Role;
import com.lumie.common.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ActivityLogCommandServiceTest {

    @Mock
    private ActivityLogRepository activityLogRepository;

    @AfterEach
    void tearDown() {
        MDC.clear();
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("activity log line restores pre-existing MDC values")
    void log_restoresMdcAfterStructuredLogLine() {
        ActivityLogCommandService service = new ActivityLogCommandService(activityLogRepository);
        TenantContextHolder.setTenant("academy-a");
        TenantContextHolder.setTenantId(7L);
        MDC.put("eventType", "previous");
        MDC.put("actorId", "previous-actor");
        MDC.put("requestId", "req-1");

        service.log(42L, "Owner", Role.OWNER.name(), ActionType.VIEW.name(),
                EntityType.STUDENT.name(), 100L, "Student A", "Viewed student");

        verify(activityLogRepository).save(any(ActivityLog.class));
        assertThat(MDC.get("eventType")).isEqualTo("previous");
        assertThat(MDC.get("actorId")).isEqualTo("previous-actor");
        assertThat(MDC.get("requestId")).isEqualTo("req-1");
        assertThat(MDC.get("action")).isNull();
    }

    @Test
    @DisplayName("legacy role aliases are logged and saved as canonical roles")
    void log_usesCanonicalActorRoleForLegacyAliases() {
        ActivityLogCommandService service = new ActivityLogCommandService(activityLogRepository);

        service.log(42L, "Instructor", "ADMIN", ActionType.VIEW.name(),
                EntityType.STUDENT.name(), 100L, "Student A", "Viewed student");

        verify(activityLogRepository).save(argThat(saved ->
                saved.getActorRole() == Role.INSTRUCTOR));
        assertThat(MDC.get("actorRole")).isNull();
    }
}
