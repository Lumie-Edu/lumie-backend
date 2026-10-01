package com.lumie.assignment.adapter.in.scheduling;

import com.lumie.assignment.application.service.AssignmentLifecycleService;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.tenant.api.TenantService;
import com.lumie.tenant.api.TenantStatus;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentDeadlineSchedulerTest {

    @Mock private AssignmentLifecycleService lifecycleService;
    @Mock private TenantService tenantService;
    @InjectMocks private AssignmentDeadlineScheduler scheduler;

    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
        MDC.clear();
    }

    @Test
    void restoresPreviousTenantAndMdcWhenTenantWorkFails() {
        TenantContextHolder.setTenant("outer");
        TenantContextHolder.setTenantId(99L);
        MDC.put("tenantSlug", "outer");
        MDC.put("tenantId", "99");
        when(tenantService.listActiveTenants()).thenReturn(List.of(
                new TenantService.TenantData(7L, "inner", "Inner", TenantStatus.ACTIVE, null, "", "")));
        when(lifecycleService.closeOverdueAssignments()).thenAnswer(invocation -> {
            assertThat(TenantContextHolder.getRequiredTenant()).isEqualTo("inner");
            assertThat(TenantContextHolder.getRequiredTenantId()).isEqualTo(7L);
            assertThat(MDC.get("tenantSlug")).isEqualTo("inner");
            throw new IllegalStateException("test failure");
        });

        scheduler.closeOverdueAssignments();

        assertThat(TenantContextHolder.getRequiredTenant()).isEqualTo("outer");
        assertThat(TenantContextHolder.getRequiredTenantId()).isEqualTo(99L);
        assertThat(MDC.get("tenantSlug")).isEqualTo("outer");
        assertThat(MDC.get("tenantId")).isEqualTo("99");
    }
}
