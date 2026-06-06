package com.lumie.activitylog.application.service;

import com.lumie.activitylog.domain.entity.ActivityLog;
import com.lumie.activitylog.domain.exception.ActivityLogErrorCode;
import com.lumie.activitylog.domain.repository.ActivityLogRepository;
import com.lumie.common.api.CursorResponse;
import com.lumie.common.auth.Role;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.UserContextHolder;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActivityLogQueryService 단위 테스트")
class ActivityLogQueryServiceTest {

    @Mock
    private ActivityLogRepository activityLogRepository;

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("OWNER 조회는 MANAGER와 INSTRUCTOR 활동만 필터링한다")
    void getLogs_owner_filtersLowerStaffRolesOnly() {
        ActivityLogQueryService service = new ActivityLogQueryService(activityLogRepository);
        SpecificationProbe probe = givenRepositoryAppliesSpecification();
        UserContextHolder.setUserRole(Role.OWNER);

        CursorResponse<?> result = service.getLogs(null, null, 30);

        assertThat(result.items()).isEmpty();
        verify(probe.actorRolePath()).in(List.of(Role.MANAGER, Role.INSTRUCTOR));
    }

    @Test
    @DisplayName("MANAGER 조회는 INSTRUCTOR 활동만 필터링한다")
    void getLogs_manager_filtersInstructorOnly() {
        ActivityLogQueryService service = new ActivityLogQueryService(activityLogRepository);
        SpecificationProbe probe = givenRepositoryAppliesSpecification();
        UserContextHolder.setUserRole(Role.MANAGER);

        CursorResponse<?> result = service.getLogs(null, null, 30);

        assertThat(result.items()).isEmpty();
        verify(probe.actorRolePath()).in(List.of(Role.INSTRUCTOR));
    }

    @Test
    @DisplayName("INSTRUCTOR는 하위 staff 직급이 없어 빈 결과를 받는다")
    void getLogs_instructor_returnsEmptyResponse() {
        ActivityLogQueryService service = new ActivityLogQueryService(activityLogRepository);
        UserContextHolder.setUserRole(Role.INSTRUCTOR);

        CursorResponse<?> result = service.getLogs(null, null, 30);

        assertThat(result.items()).isEmpty();
        assertThat(result.nextCursor()).isNull();
        assertThat(result.limit()).isEqualTo(30);
        verifyNoInteractions(activityLogRepository);
    }

    @Test
    @DisplayName("STUDENT는 활동 기록 감시 탭을 조회할 수 없다")
    void getLogs_student_throwsInsufficientRole() {
        ActivityLogQueryService service = new ActivityLogQueryService(activityLogRepository);
        UserContextHolder.setUserRole(Role.STUDENT);

        assertThatThrownBy(() -> service.getLogs(null, null, 30))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ActivityLogErrorCode.INSUFFICIENT_ROLE));
        verifyNoInteractions(activityLogRepository);
    }

    @Test
    @DisplayName("역할 컨텍스트가 없으면 활동 기록 감시 탭을 조회할 수 없다")
    void getLogs_missingRole_throwsInsufficientRole() {
        ActivityLogQueryService service = new ActivityLogQueryService(activityLogRepository);

        assertThatThrownBy(() -> service.getLogs(null, null, 30))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ActivityLogErrorCode.INSUFFICIENT_ROLE));
        verifyNoInteractions(activityLogRepository);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private SpecificationProbe givenRepositoryAppliesSpecification() {
        Root<ActivityLog> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<Role> actorRolePath = mock(Path.class);
        Predicate predicate = mock(Predicate.class);

        when(root.<Role>get("actorRole")).thenReturn(actorRolePath);
        when(actorRolePath.in(any(Collection.class))).thenReturn(predicate);
        when(cb.and(any(Predicate[].class))).thenReturn(predicate);
        when(activityLogRepository.findBy(any(Specification.class), any(Function.class))).thenAnswer(inv -> {
            Specification<ActivityLog> spec = inv.getArgument(0);
            spec.toPredicate(root, query, cb);
            return List.of();
        });

        return new SpecificationProbe(actorRolePath);
    }

    private record SpecificationProbe(Path<Role> actorRolePath) {
    }
}
