package com.lumie.exam.adapter.in.web;

import com.lumie.common.auth.Role;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.CommonErrorCode;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.exam.application.service.ChoiceDistributionQueryService;
import com.lumie.exam.application.service.ClassComparisonQueryService;
import com.lumie.exam.application.service.ExamSummaryStatisticsService;
import com.lumie.exam.application.service.ItemAnalysisQueryService;
import com.lumie.exam.application.service.ResultQueryService;
import com.lumie.exam.application.service.StudentAnalyticsQueryService;
import com.lumie.exam.application.service.StudentGradeQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StatisticsControllerTest {

    @Mock private ExamSummaryStatisticsService examSummaryStatisticsService;
    @Mock private ChoiceDistributionQueryService choiceDistributionQueryService;
    @Mock private ClassComparisonQueryService classComparisonQueryService;
    @Mock private ItemAnalysisQueryService itemAnalysisQueryService;
    @Mock private StudentAnalyticsQueryService studentAnalyticsQueryService;
    @Mock private StudentGradeQueryService studentGradeQueryService;
    @Mock private ResultQueryService resultQueryService;
    @InjectMocks private StatisticsController controller;

    @BeforeEach
    void setStudentRole() {
        UserContextHolder.setUserRole(Role.STUDENT);
    }

    @AfterEach
    void clearRole() {
        UserContextHolder.clear();
    }

    @Test
    void studentSpecificStatisticsCheckOwnershipBeforeQuerying() {
        willThrow(new BusinessException(CommonErrorCode.FORBIDDEN))
                .given(resultQueryService).requireStudentAccess(456L);

        assertThatThrownBy(() -> controller.getStudentRank(456L, 5L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> controller.getStabilityIndex(456L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> controller.getTypeGrowthTrend(456L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> controller.getNormalizedScores(456L)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(studentAnalyticsQueryService);
    }

    @Test
    void aggregateStatisticsRequireStaff() {
        assertThatThrownBy(() -> controller.getExamStatistics(5L, null, null, null))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(examSummaryStatisticsService);
    }
}
