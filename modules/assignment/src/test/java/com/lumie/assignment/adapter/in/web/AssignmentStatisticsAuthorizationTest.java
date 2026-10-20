package com.lumie.assignment.adapter.in.web;

import static org.mockito.Mockito.verifyNoInteractions;

import com.lumie.common.auth.Role;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.assignment.application.service.AssignmentStatisticsQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssignmentStatisticsAuthorizationTest {

    @Mock private AssignmentStatisticsQueryService queryService;

    @AfterEach
    void clearUserContext() {
        UserContextHolder.clear();
    }

    @Test
    void studentCannotReadAssignmentStatisticsBeforeQueryService() {
        UserContextHolder.setUserRole(Role.STUDENT);
        AssignmentStatisticsController controller = new AssignmentStatisticsController(queryService);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.getDashboardStatistics(null))
                .isInstanceOf(com.lumie.common.exception.BusinessException.class);

        verifyNoInteractions(queryService);
    }

    @Test
    void instructorCanReadAssignmentStatistics() {
        UserContextHolder.setUserRole(Role.INSTRUCTOR);
        AssignmentStatisticsController controller = new AssignmentStatisticsController(queryService);

        org.assertj.core.api.Assertions.assertThat(controller.getDashboardStatistics(null).getStatusCode().value())
                .isEqualTo(200);
        org.assertj.core.api.Assertions.assertThat(controller.getDashboardStatistics(6).getStatusCode().value())
                .isEqualTo(200);

        org.mockito.Mockito.verify(queryService).getDashboardStatistics();
        org.mockito.Mockito.verify(queryService).getDashboardStatistics(6);
    }
}
