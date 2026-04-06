package com.lumie.journal.domain.entity;

import com.lumie.journal.domain.vo.JournalStatus;
import com.lumie.journal.domain.vo.JournalVisibility;
import com.lumie.journal.domain.vo.ProcessingStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClassJournal 도메인 엔티티 테스트")
class ClassJournalTest {

    @Test
    @DisplayName("create() 호출 시 기본 상태는 ACTIVE이다")
    void create_defaultStatusIsActive() {
        ClassJournal journal = ClassJournal.create(
                1L, 1L, 10L, LocalDate.of(2026, 4, 1),
                "수학 수업일지", "미적분 기초 학습", List.of("수학", "미적분"),
                "교재 23~25페이지", JournalVisibility.TEACHER_ONLY, null);

        assertThat(journal.getStatus()).isEqualTo(JournalStatus.ACTIVE);
        assertThat(journal.getProcessingStatus()).isEqualTo(ProcessingStatus.COMPLETED);
        assertThat(journal.getVisibility()).isEqualTo(JournalVisibility.TEACHER_ONLY);
        assertThat(journal.getTitle()).isEqualTo("수학 수업일지");
        assertThat(journal.getContent()).isEqualTo("미적분 기초 학습");
        assertThat(journal.getClassId()).isEqualTo(1L);
        assertThat(journal.getAcademyId()).isEqualTo(1L);
        assertThat(journal.getTeacherId()).isEqualTo(10L);
        assertThat(journal.getTopicsCovered()).containsExactly("수학", "미적분");
        assertThat(journal.getHomeworkNote()).isEqualTo("교재 23~25페이지");
    }

    @Test
    @DisplayName("create() 시 visibility가 null이면 TEACHER_ONLY가 기본값이다")
    void create_nullVisibility_defaultsToTeacherOnly() {
        ClassJournal journal = ClassJournal.create(
                1L, 1L, 10L, LocalDate.of(2026, 4, 1),
                "수업일지", null, null, null, null, null);

        assertThat(journal.getVisibility()).isEqualTo(JournalVisibility.TEACHER_ONLY);
    }

    @Test
    @DisplayName("close() 호출 시 상태가 CLOSED로 변경된다")
    void close_statusChangesToClosed() {
        ClassJournal journal = buildJournal();

        journal.close();

        assertThat(journal.getStatus()).isEqualTo(JournalStatus.CLOSED);
    }

    @Test
    @DisplayName("activate() 호출 시 상태가 ACTIVE로 변경된다")
    void activate_statusChangesToActive() {
        ClassJournal journal = buildJournal();
        journal.close();

        journal.activate();

        assertThat(journal.getStatus()).isEqualTo(JournalStatus.ACTIVE);
    }

    @Test
    @DisplayName("markDraft() 호출 시 상태가 DRAFT로 변경된다")
    void markDraft_statusChangesToDraft() {
        ClassJournal journal = buildJournal();

        journal.markDraft();

        assertThat(journal.getStatus()).isEqualTo(JournalStatus.DRAFT);
    }

    @Test
    @DisplayName("isActive()는 ACTIVE 상태일 때만 true를 반환한다")
    void isActive_returnsTrueOnlyWhenActive() {
        ClassJournal journal = buildJournal();
        assertThat(journal.isActive()).isTrue();

        journal.close();
        assertThat(journal.isActive()).isFalse();

        journal.activate();
        assertThat(journal.isActive()).isTrue();
    }

    @Test
    @DisplayName("updateInfo() 호출 시 필드가 업데이트된다")
    void updateInfo_updatesFields() {
        ClassJournal journal = buildJournal();

        journal.updateInfo("변경된 제목", "변경된 내용", List.of("영어", "문법"),
                "변경된 숙제", JournalVisibility.PARENT, LocalDate.of(2026, 4, 2),
                List.of(100L, 200L));

        assertThat(journal.getTitle()).isEqualTo("변경된 제목");
        assertThat(journal.getContent()).isEqualTo("변경된 내용");
        assertThat(journal.getTopicsCovered()).containsExactly("영어", "문법");
        assertThat(journal.getHomeworkNote()).isEqualTo("변경된 숙제");
        assertThat(journal.getVisibility()).isEqualTo(JournalVisibility.PARENT);
        assertThat(journal.getLessonDate()).isEqualTo(LocalDate.of(2026, 4, 2));
        assertThat(journal.getAttachments()).containsExactly(100L, 200L);
    }

    @Test
    @DisplayName("create() 시 topicsCovered와 homeworkNote가 null이어도 생성 가능하다")
    void create_nullOptionalFields_succeeds() {
        ClassJournal journal = ClassJournal.create(
                1L, 1L, 10L, LocalDate.now(),
                "간단 수업일지", null, null, null, null, null);

        assertThat(journal.getTopicsCovered()).isNull();
        assertThat(journal.getHomeworkNote()).isNull();
        assertThat(journal.getContent()).isNull();
    }

    private ClassJournal buildJournal() {
        return ClassJournal.create(
                1L, 1L, 10L, LocalDate.of(2026, 4, 1),
                "수학 수업일지", "미적분 기초 학습", List.of("수학", "미적분"),
                "교재 23~25페이지", JournalVisibility.TEACHER_ONLY, null);
    }
}
