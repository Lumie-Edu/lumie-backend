package com.lumie.assignment.domain.entity;

import com.lumie.assignment.domain.vo.AssignmentStatus;
import com.lumie.assignment.domain.vo.AssignmentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Assignment 도메인 엔티티 테스트")
class AssignmentEntityTest {

    @Test
    @DisplayName("create() 호출 시 기본 상태는 ACTIVE이다")
    void create_defaultStatusIsActive() {
        Assignment entity = Assignment.create(
                1L, "수학 숙제", "1장~3장 풀기",
                AssignmentType.HOMEWORK, LocalDateTime.of(2026, 4, 15, 23, 59).toInstant(ZoneOffset.UTC),
                100, List.of(1L, 2L));

        assertThat(entity.getStatus()).isEqualTo(AssignmentStatus.ACTIVE);
        assertThat(entity.getTitle()).isEqualTo("수학 숙제");
        assertThat(entity.getDescription()).isEqualTo("1장~3장 풀기");
        assertThat(entity.getClassId()).isEqualTo(1L);
        assertThat(entity.getType()).isEqualTo(AssignmentType.HOMEWORK);
        assertThat(entity.getMaxScore()).isEqualTo(100);
        assertThat(entity.getAttachments()).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("updateInfo() 호출 시 필드가 업데이트된다")
    void updateInfo_updatesFields() {
        Assignment entity = buildEntity();

        entity.updateInfo("영어 에세이", "자유 주제 에세이 작성",
                AssignmentType.ESSAY, LocalDateTime.of(2026, 5, 1, 23, 59).toInstant(ZoneOffset.UTC),
                50, List.of(3L));

        assertThat(entity.getTitle()).isEqualTo("영어 에세이");
        assertThat(entity.getDescription()).isEqualTo("자유 주제 에세이 작성");
        assertThat(entity.getType()).isEqualTo(AssignmentType.ESSAY);
        assertThat(entity.getMaxScore()).isEqualTo(50);
        assertThat(entity.getAttachments()).containsExactly(3L);
    }

    @Test
    @DisplayName("close() 호출 시 상태가 CLOSED로 변경된다")
    void close_statusChangesToClosed() {
        Assignment entity = buildEntity();

        entity.close();

        assertThat(entity.getStatus()).isEqualTo(AssignmentStatus.CLOSED);
    }

    @Test
    @DisplayName("activate() 호출 시 상태가 ACTIVE로 변경된다")
    void activate_statusChangesToActive() {
        Assignment entity = buildEntity();
        entity.close();

        entity.activate();

        assertThat(entity.getStatus()).isEqualTo(AssignmentStatus.ACTIVE);
    }

    @Test
    @DisplayName("isActive()는 ACTIVE 상태일 때만 true를 반환한다")
    void isActive_returnsTrueOnlyWhenActive() {
        Assignment entity = buildEntity();
        assertThat(entity.isActive()).isTrue();

        entity.close();
        assertThat(entity.isActive()).isFalse();

        entity.activate();
        assertThat(entity.isActive()).isTrue();
    }

    @Test
    @DisplayName("create() 시 type이 null이면 HOMEWORK가 기본값이다")
    void create_nullType_defaultsToHomework() {
        Assignment entity = Assignment.create(
                1L, "과제", null, null,
                null, null, null);

        assertThat(entity.getType()).isEqualTo(AssignmentType.HOMEWORK);
        assertThat(entity.getDescription()).isNull();
        assertThat(entity.getDueDate()).isNull();
    }

    private Assignment buildEntity() {
        return Assignment.create(
                1L, "수학 숙제", "1장~3장 풀기",
                AssignmentType.HOMEWORK, LocalDateTime.of(2026, 4, 15, 23, 59).toInstant(ZoneOffset.UTC),
                100, null);
    }
}
