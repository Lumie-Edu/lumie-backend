package com.lumie.cls.domain.entity;

import com.lumie.cls.domain.vo.ClassStatus;
import com.lumie.cls.domain.vo.SchedulePattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClassEntity 도메인 엔티티 테스트")
class ClassEntityTest {

    @Test
    @DisplayName("create() 호출 시 기본 상태는 PENDING이다")
    void create_defaultStatusIsPending() {
        ClassEntity entity = ClassEntity.create(
                1L, "수학 기초반", "수학", 10L, 30, 100_000L,
                new SchedulePattern(List.of("MON", "WED"), "14:00", "15:30"),
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30), "설명");

        assertThat(entity.getStatus()).isEqualTo(ClassStatus.PENDING);
        assertThat(entity.getName()).isEqualTo("수학 기초반");
        assertThat(entity.getSubject()).isEqualTo("수학");
        assertThat(entity.getAcademyId()).isEqualTo(1L);
        assertThat(entity.getTeacherId()).isEqualTo(10L);
        assertThat(entity.getMaxStudents()).isEqualTo(30);
        assertThat(entity.getTuitionAmount()).isEqualTo(100_000L);
    }

    @Test
    @DisplayName("activate() 호출 시 상태가 ACTIVE로 변경된다")
    void activate_statusChangesToActive() {
        ClassEntity entity = buildEntity();

        entity.activate();

        assertThat(entity.getStatus()).isEqualTo(ClassStatus.ACTIVE);
    }

    @Test
    @DisplayName("close() 호출 시 상태가 CLOSED로 변경된다")
    void close_statusChangesToClosed() {
        ClassEntity entity = buildEntity();
        entity.activate();

        entity.close();

        assertThat(entity.getStatus()).isEqualTo(ClassStatus.CLOSED);
    }

    @Test
    @DisplayName("isActive()는 ACTIVE 상태일 때만 true를 반환한다")
    void isActive_returnsTrueOnlyWhenActive() {
        ClassEntity entity = buildEntity();
        assertThat(entity.isActive()).isFalse();

        entity.activate();
        assertThat(entity.isActive()).isTrue();

        entity.close();
        assertThat(entity.isActive()).isFalse();
    }

    @Test
    @DisplayName("updateInfo() 호출 시 필드가 업데이트된다")
    void updateInfo_updatesFields() {
        ClassEntity entity = buildEntity();

        entity.updateInfo("영어 고급반", "영어", 20L, 25, 150_000L,
                new SchedulePattern(List.of("TUE", "THU"), "10:00", "11:30"),
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 7, 31), "변경된 설명");

        assertThat(entity.getName()).isEqualTo("영어 고급반");
        assertThat(entity.getSubject()).isEqualTo("영어");
        assertThat(entity.getTeacherId()).isEqualTo(20L);
        assertThat(entity.getMaxStudents()).isEqualTo(25);
        assertThat(entity.getTuitionAmount()).isEqualTo(150_000L);
        assertThat(entity.getDescription()).isEqualTo("변경된 설명");
    }

    @Test
    @DisplayName("create() 시 schedulePattern이 null이어도 생성 가능하다")
    void create_nullSchedulePattern_succeeds() {
        ClassEntity entity = ClassEntity.create(
                1L, "반 이름", null, null, 0, 0L,
                null, LocalDate.now(), LocalDate.now().plusMonths(3), null);

        assertThat(entity.getSchedulePattern()).isNull();
        assertThat(entity.getSubject()).isNull();
    }

    private ClassEntity buildEntity() {
        return ClassEntity.create(
                1L, "수학 기초반", "수학", 10L, 30, 100_000L,
                new SchedulePattern(List.of("MON", "WED"), "14:00", "15:30"),
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30), "설명");
    }
}
