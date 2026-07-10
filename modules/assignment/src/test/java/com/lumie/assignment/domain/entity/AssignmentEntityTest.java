package com.lumie.assignment.domain.entity;

import com.lumie.assignment.domain.vo.AssignmentStatus;
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
                1L, LocalDateTime.of(2026, 4, 15, 23, 59).toInstant(ZoneOffset.UTC),
                100, List.of(1L, 2L));

        assertThat(entity.getStatus()).isEqualTo(AssignmentStatus.ACTIVE);
        assertThat(entity.getClassId()).isEqualTo(1L);
        assertThat(entity.getMaxScore()).isEqualTo(100);
        assertThat(entity.getAttachments()).containsExactly(1L, 2L);
        assertThat(entity.isShowScore()).isTrue();
        assertThat(entity.isShowPassFail()).isTrue();
        assertThat(entity.isShowGrade()).isFalse();
        assertThat(entity.isShowQuestionResults()).isTrue();
        assertThat(entity.isShowCorrectAnswers()).isFalse();
    }

    @Test
    @DisplayName("updateInfo() 호출 시 필드가 업데이트된다")
    void updateInfo_updatesFields() {
        Assignment entity = buildEntity();

        entity.updateInfo(LocalDateTime.of(2026, 5, 1, 23, 59).toInstant(ZoneOffset.UTC),
                50, List.of(3L));

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

    private Assignment buildEntity() {
        return Assignment.create(
                1L, LocalDateTime.of(2026, 4, 15, 23, 59).toInstant(ZoneOffset.UTC),
                100, null);
    }
}
