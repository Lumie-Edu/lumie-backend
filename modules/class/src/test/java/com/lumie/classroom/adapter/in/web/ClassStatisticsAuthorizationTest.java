package com.lumie.classroom.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import com.lumie.common.auth.Role;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.classroom.application.service.ClassStatisticsQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClassStatisticsAuthorizationTest {

    @Mock private ClassStatisticsQueryService queryService;

    @AfterEach
    void clearUserContext() {
        UserContextHolder.clear();
    }

    @Test
    void studentCannotReadClassStatisticsBeforeQueryService() {
        UserContextHolder.setUserRole(Role.STUDENT);
        ClassStatisticsController controller = new ClassStatisticsController(queryService);

        org.assertj.core.api.Assertions.assertThatThrownBy(controller::getDashboardStatistics)
                .isInstanceOf(BusinessException.class);

        verifyNoInteractions(queryService);
    }

    @Test
    void instructorCanReadClassStatistics() {
        UserContextHolder.setUserRole(Role.INSTRUCTOR);
        ClassStatisticsController controller = new ClassStatisticsController(queryService);

        assertThat(controller.getDashboardStatistics().getStatusCode().value()).isEqualTo(200);

        org.mockito.Mockito.verify(queryService).getDashboardStatistics();
    }
}
