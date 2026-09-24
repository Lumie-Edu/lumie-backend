package com.lumie.exam.adapter.in.event;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.application.service.OmrSubmissionService;
import com.lumie.student.api.StudentRegisteredEvent;
import com.lumie.tenant.api.TenantService;
import com.lumie.tenant.api.TenantStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudentRegisteredListener 단위 테스트")
class StudentRegisteredListenerTest {

    @Mock
    private TenantService tenantService;

    @Mock
    private OmrSubmissionService omrSubmissionService;

    @InjectMocks
    private StudentRegisteredListener listener;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("요청 thread 밖에서는 이벤트의 tenantId까지 복원해 submission backfill을 실행한다")
    void onStudentRegistered_restoresTenantSlugAndIdForReplay() {
        given(omrSubmissionService.linkSingleUnmatchedByPhone("01012345678", 100L)).willAnswer(invocation -> {
            assertThat(TenantContextHolder.getTenant()).isEqualTo("inst-acme");
            assertThat(TenantContextHolder.getTenantId()).isEqualTo(7L);
            return 1;
        });

        listener.onStudentRegistered(new StudentRegisteredEvent(100L, "01012345678", "inst-acme", 7L));

        verify(omrSubmissionService).linkSingleUnmatchedByPhone("01012345678", 100L);
        assertThat(TenantContextHolder.getTenant()).isNull();
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    @DisplayName("기존 thread context가 있어도 event snapshot으로 덮어 submission backfill을 실행한다")
    void onStudentRegistered_usesEventTenantSnapshotOverAmbientContext() {
        TenantContextHolder.setTenant("inst-stale");
        TenantContextHolder.setTenantId(99L);
        given(omrSubmissionService.linkSingleUnmatchedByPhone("01012345678", 100L)).willAnswer(invocation -> {
            assertThat(TenantContextHolder.getTenant()).isEqualTo("inst-acme");
            assertThat(TenantContextHolder.getTenantId()).isEqualTo(7L);
            return 1;
        });

        listener.onStudentRegistered(new StudentRegisteredEvent(100L, "01012345678", "inst-acme", 7L));

        verify(omrSubmissionService).linkSingleUnmatchedByPhone("01012345678", 100L);
        assertThat(TenantContextHolder.getTenant()).isEqualTo("inst-stale");
        assertThat(TenantContextHolder.getTenantId()).isEqualTo(99L);
    }

    @Test
    @DisplayName("tenantSlug가 없는 event는 ambient context로 실행하지 않고 실패한다")
    void onStudentRegistered_requiresTenantSlug() {
        TenantContextHolder.setTenant("inst-stale");
        TenantContextHolder.setTenantId(99L);

        assertThatThrownBy(() ->
                listener.onStudentRegistered(new StudentRegisteredEvent(100L, "01012345678", null, 7L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tenantSlug is required");

        verifyNoInteractions(omrSubmissionService);
    }

    @Test
    @DisplayName("요청 thread 밖에서 tenantId가 없는 이전 event는 slug로 tenantId를 복원한다")
    void onStudentRegistered_resolvesTenantIdForLegacyReplay() {
        given(tenantService.getTenantBySlug("inst-acme")).willReturn(Optional.of(tenantData(7L, "inst-acme")));

        listener.onStudentRegistered(new StudentRegisteredEvent(100L, "01012345678", "inst-acme", null));

        verify(tenantService).getTenantBySlug("inst-acme");
        verify(omrSubmissionService).linkSingleUnmatchedByPhone("01012345678", 100L);
    }

    @Test
    @DisplayName("요청 thread 밖에서 tenantId가 없고 slug도 해석할 수 없으면 실패한다")
    void onStudentRegistered_failsWhenLegacyTenantSlugCannotResolve() {
        given(tenantService.getTenantBySlug("inst-missing")).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                listener.onStudentRegistered(new StudentRegisteredEvent(100L, "01012345678", "inst-missing", null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tenant slug cannot be resolved");

        verifyNoInteractions(omrSubmissionService);
    }

    @Test
    @DisplayName("submission backfill 예외는 event publication 재시도가 가능하도록 전파한다")
    void onStudentRegistered_propagatesBackfillFailure() {
        TenantContextHolder.withinContext("inst-acme", 7L, () -> {
            given(omrSubmissionService.linkSingleUnmatchedByPhone("01012345678", 100L))
                    .willThrow(new IllegalStateException("optimistic lock"));

            assertThatThrownBy(() ->
                    listener.onStudentRegistered(new StudentRegisteredEvent(100L, "01012345678", "inst-acme", 7L)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("optimistic lock");
        });
    }

    private static TenantService.TenantData tenantData(Long id, String slug) {
        return new TenantService.TenantData(
                id,
                slug,
                "테스트 학원",
                TenantStatus.ACTIVE,
                null,
                null,
                null,
                "2026-01-01T00:00:00Z",
                "2026-01-01T00:00:00Z"
        );
    }
}
