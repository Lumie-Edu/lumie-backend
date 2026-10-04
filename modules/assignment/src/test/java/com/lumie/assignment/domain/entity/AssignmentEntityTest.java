package com.lumie.assignment.domain.entity;

import com.lumie.assignment.domain.vo.AssignmentStatus;
import com.lumie.assignment.domain.exception.AssignmentErrorCode;
import com.lumie.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Assignment 도메인 엔티티 테스트")
class AssignmentEntityTest {

    @Test
    @DisplayName("create() 호출 시 기본 상태는 ACTIVE이다")
    void create_defaultStatusIsActive() {
        Assignment entity = Assignment.create(
                LocalDateTime.of(2026, 4, 15, 23, 59).toInstant(ZoneOffset.UTC),
                100, List.of(1L, 2L));

        assertThat(entity.getStatus()).isEqualTo(AssignmentStatus.ACTIVE);
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
    @DisplayName("updateInfo()는 null 마감일을 허용하지 않는다")
    void updateInfo_nullDueDate_throwsException() {
        Assignment entity = buildEntity();

        assertThatThrownBy(() -> entity.updateInfo(null, 50, List.of(3L)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(AssignmentErrorCode.DUE_DATE_REQUIRED));
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
    @DisplayName("closeIfOverdue()는 마감 시간이 지난 ACTIVE 과제를 CLOSED로 변경한다")
    void closeIfOverdue_overdueActive_closes() {
        Assignment entity = Assignment.create(
                Instant.parse("2026-04-15T23:59:00Z"),
                100, null);

        boolean changed = entity.closeIfOverdue(Instant.parse("2026-04-16T00:00:00Z"));

        assertThat(changed).isTrue();
        assertThat(entity.getStatus()).isEqualTo(AssignmentStatus.CLOSED);
    }

    @Test
    @DisplayName("closeIfOverdue()는 마감 전 ACTIVE 과제를 변경하지 않는다")
    void closeIfOverdue_futureActive_keepsActive() {
        Assignment entity = Assignment.create(
                Instant.parse("2026-04-15T23:59:00Z"),
                100, null);

        boolean changed = entity.closeIfOverdue(Instant.parse("2026-04-15T23:58:59Z"));

        assertThat(changed).isFalse();
        assertThat(entity.getStatus()).isEqualTo(AssignmentStatus.ACTIVE);
    }

    private Assignment buildEntity() {
        return Assignment.create(
                LocalDateTime.of(2026, 4, 15, 23, 59).toInstant(ZoneOffset.UTC),
                100, null);
    }
}
