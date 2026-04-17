package com.lumie.cls.domain.entity;

import com.lumie.cls.domain.vo.SchedulePattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClassEntity 도메인 엔티티 테스트")
class ClassEntityTest {

    @Test
    @DisplayName("create() 호출 시 필드가 올바르게 설정된다")
    void create_fieldsAreSetCorrectly() {
        ClassEntity entity = ClassEntity.create(
                1L, "수학 기초반", 10L,
                new SchedulePattern(List.of("MON", "WED"), "14:00", "15:30"),
                "설명");

        assertThat(entity.getName()).isEqualTo("수학 기초반");
        assertThat(entity.getAcademyId()).isEqualTo(1L);
        assertThat(entity.getTeacherId()).isEqualTo(10L);
        assertThat(entity.getDescription()).isEqualTo("설명");
    }

    @Test
    @DisplayName("updateInfo() 호출 시 필드가 업데이트된다")
    void updateInfo_updatesFields() {
        ClassEntity entity = buildEntity();

        entity.updateInfo("영어 고급반", 20L,
                new SchedulePattern(List.of("TUE", "THU"), "10:00", "11:30"),
                "변경된 설명");

        assertThat(entity.getName()).isEqualTo("영어 고급반");
        assertThat(entity.getTeacherId()).isEqualTo(20L);
        assertThat(entity.getDescription()).isEqualTo("변경된 설명");
    }

    @Test
    @DisplayName("create() 시 schedulePattern이 null이어도 생성 가능하다")
    void create_nullSchedulePattern_succeeds() {
        ClassEntity entity = ClassEntity.create(
                1L, "반 이름", null,
                null, null);

        assertThat(entity.getSchedulePattern()).isNull();
    }

    private ClassEntity buildEntity() {
        return ClassEntity.create(
                1L, "수학 기초반", 10L,
                new SchedulePattern(List.of("MON", "WED"), "14:00", "15:30"),
                "설명");
    }
}
