package com.lumie.content.application.service;

import com.lumie.content.domain.repository.ReservationRepository;
import com.lumie.content.domain.repository.ScheduleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeletionReadinessService 테스트")
class DeletionReadinessServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @InjectMocks
    private DeletionReadinessService service;

    @Test
    @DisplayName("학생에게 예정 또는 진행 상담 예약이 있으면 true를 반환한다")
    void hasActiveOrUpcomingReservationsForStudent_returnsRepositoryResult() {
        given(reservationRepository.existsActiveOrUpcomingByStudentId(42L))
                .willReturn(true);

        assertThat(service.hasActiveOrUpcomingReservationsForStudent(42L)).isTrue();
    }

    @Test
    @DisplayName("사용자에게 오늘 이후 상담 일정이 있으면 true를 반환한다")
    void hasUpcomingSchedulesForUser_returnsRepositoryResult() {
        given(scheduleRepository.existsByUserIdAndDateGreaterThanEqual(eq(7L), any()))
                .willReturn(true);

        assertThat(service.hasUpcomingSchedulesForUser(7L)).isTrue();
    }
}
