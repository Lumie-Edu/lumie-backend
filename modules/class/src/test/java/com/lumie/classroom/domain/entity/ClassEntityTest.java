package com.lumie.classroom.domain.entity;

import com.lumie.classroom.domain.vo.SchedulePattern;
import com.lumie.common.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClassEntity 도메인 엔티티 테스트")
class ClassEntityTest {

    @BeforeEach
    void setUpTenantContext() {
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("create() 호출 시 필드가 올바르게 설정된다")
    void create_fieldsAreSetCorrectly() {
        ClassEntity entity = ClassEntity.create(
                "수학 기초반", 10L,
                new SchedulePattern(List.of(
                        new SchedulePattern.ScheduleSession("MON", "14:00", "15:30"),
                        new SchedulePattern.ScheduleSession("WED", "14:00", "15:30"))),
                "설명");

        assertThat(entity.getName()).isEqualTo("수학 기초반");
        assertThat(entity.getTeacherId()).isEqualTo(10L);
        assertThat(entity.getDescription()).isEqualTo("설명");
    }

    @Test
    @DisplayName("요일별 schedule session을 보존한다")
    void schedulePattern_perDaySessionsArePreserved() {
        SchedulePattern schedulePattern = new SchedulePattern(List.of(
                new SchedulePattern.ScheduleSession("MON", "14:00", "15:30"),
                new SchedulePattern.ScheduleSession("WED", "14:00", "15:30")));

        assertThat(schedulePattern.sessions())
                .extracting("day", "startTime", "endTime")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("MON", "14:00", "15:30"),
                        org.assertj.core.groups.Tuple.tuple("WED", "14:00", "15:30")
                );
    }

    @Test
    @DisplayName("같은 시간의 요일별 schedulePattern은 session 목록만 보존한다")
    void schedulePattern_sessionsWithSharedTimeArePreserved() {
        SchedulePattern schedulePattern = new SchedulePattern(
                List.of(
                        new SchedulePattern.ScheduleSession("WED", "15:00", "16:00"),
                        new SchedulePattern.ScheduleSession("FRI", "15:00", "16:00")
                )
        );

        assertThat(schedulePattern.sessions())
                .extracting("day", "startTime", "endTime")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("WED", "15:00", "16:00"),
                        org.assertj.core.groups.Tuple.tuple("FRI", "15:00", "16:00")
                );
    }

    @Test
    @DisplayName("다른 시간의 요일별 schedulePattern은 session별 시간을 보존한다")
    void schedulePattern_sessionsWithMixedTimeArePreserved() {
        SchedulePattern schedulePattern = new SchedulePattern(
                List.of(
                        new SchedulePattern.ScheduleSession("WED", "15:00", "16:00"),
                        new SchedulePattern.ScheduleSession("FRI", "18:00", "19:00")
                )
        );

        assertThat(schedulePattern.sessions())
                .extracting("day", "startTime", "endTime")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("WED", "15:00", "16:00"),
                        org.assertj.core.groups.Tuple.tuple("FRI", "18:00", "19:00")
                );
    }

    @Test
    @DisplayName("updateInfo() 호출 시 필드가 업데이트된다")
    void updateInfo_updatesFields() {
        ClassEntity entity = buildEntity();

        entity.updateInfo("영어 고급반", 20L,
                new SchedulePattern(List.of(
                        new SchedulePattern.ScheduleSession("TUE", "10:00", "11:30"),
                        new SchedulePattern.ScheduleSession("THU", "10:00", "11:30"))),
                "변경된 설명");

        assertThat(entity.getName()).isEqualTo("영어 고급반");
        assertThat(entity.getTeacherId()).isEqualTo(20L);
        assertThat(entity.getDescription()).isEqualTo("변경된 설명");
    }

    @Test
    @DisplayName("create() 시 schedulePattern이 null이어도 생성 가능하다")
    void create_nullSchedulePattern_succeeds() {
        ClassEntity entity = ClassEntity.create(
                "반 이름", null,
                null, null);

        assertThat(entity.getSchedulePattern()).isNull();
    }

    private ClassEntity buildEntity() {
        return ClassEntity.create(
                "수학 기초반", 10L,
                new SchedulePattern(List.of(
                        new SchedulePattern.ScheduleSession("MON", "14:00", "15:30"),
                        new SchedulePattern.ScheduleSession("WED", "14:00", "15:30"))),
                "설명");
    }
}
