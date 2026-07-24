package com.lumie.classroom.adapter.in.internal;

import com.lumie.classroom.application.service.ClassCommandService;
import com.lumie.classroom.application.service.ClassQueryService;
import com.lumie.common.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ClassServiceAdapterTest {

    @Mock private ClassQueryService classQueryService;
    @Mock private ClassCommandService classCommandService;
    @InjectMocks private ClassServiceAdapter classServiceAdapter;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void enrollmentLookups_setAndRestoreFullTenantContext() {
        TenantContextHolder.setTenant("original-tenant");
        TenantContextHolder.setTenantId(99L);
        given(classQueryService.getActiveEnrolledStudentIds()).willAnswer(invocation -> {
            assertThat(TenantContextHolder.getRequiredTenant()).isEqualTo("target-tenant");
            assertThat(TenantContextHolder.getRequiredTenantId()).isEqualTo(1L);
            return List.of(10L);
        });

        List<Long> result = classServiceAdapter.getActiveEnrolledStudentIds("target-tenant", 1L);

        assertThat(result).containsExactly(10L);
        assertThat(TenantContextHolder.getRequiredTenant()).isEqualTo("original-tenant");
        assertThat(TenantContextHolder.getRequiredTenantId()).isEqualTo(99L);

        given(classQueryService.getEnrolledStudentIds(7L)).willAnswer(invocation -> {
            assertThat(TenantContextHolder.getRequiredTenant()).isEqualTo("target-tenant");
            assertThat(TenantContextHolder.getRequiredTenantId()).isEqualTo(1L);
            return List.of(20L);
        });

        List<Long> classResult = classServiceAdapter.getEnrolledStudentIds("target-tenant", 1L, 7L);

        assertThat(classResult).containsExactly(20L);
        assertThat(TenantContextHolder.getRequiredTenant()).isEqualTo("original-tenant");
        assertThat(TenantContextHolder.getRequiredTenantId()).isEqualTo(99L);
    }
}
