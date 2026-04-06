package com.lumie.journal.application.service;

import com.lumie.journal.application.dto.JournalResponse;
import com.lumie.journal.domain.entity.ClassJournal;
import com.lumie.journal.domain.exception.JournalNotFoundException;
import com.lumie.journal.domain.repository.JournalRepository;
import com.lumie.journal.domain.vo.JournalSearchCriteria;
import com.lumie.journal.domain.vo.JournalStatus;
import com.lumie.journal.domain.vo.JournalVisibility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("JournalQueryService 테스트")
class JournalQueryServiceTest {

    @Mock private JournalRepository journalRepository;
    @InjectMocks private JournalQueryService queryService;

    @Test
    @DisplayName("getJournal 성공 시 JournalResponse를 반환한다")
    void getJournal_success() throws Exception {
        ClassJournal journal = buildJournal(1L);
        given(journalRepository.findById(1L)).willReturn(Optional.of(journal));

        JournalResponse response = queryService.getJournal(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("수학 수업일지");
    }

    @Test
    @DisplayName("getJournal 존재하지 않으면 JournalNotFoundException이 발생한다")
    void getJournal_notFound_throwsException() {
        given(journalRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> queryService.getJournal(999L))
                .isInstanceOf(JournalNotFoundException.class);
    }

    @Test
    @DisplayName("listJournals 성공 시 페이지네이션된 결과를 반환한다")
    void listJournals_success() throws Exception {
        ClassJournal journal = buildJournal(1L);
        Pageable pageable = PageRequest.of(0, 20);
        Page<ClassJournal> page = new PageImpl<>(List.of(journal), pageable, 1);

        given(journalRepository.findAll(any(JournalSearchCriteria.class), eq(pageable))).willReturn(page);

        Page<JournalResponse> response = queryService.listJournals(
                1L, null, null, null, null, null, null, pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).title()).isEqualTo("수학 수업일지");
    }

    @Test
    @DisplayName("listJournals 필터 조건을 JournalSearchCriteria로 전달한다")
    void listJournals_withFilters() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        Page<ClassJournal> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        given(journalRepository.findAll(any(JournalSearchCriteria.class), eq(pageable))).willReturn(emptyPage);

        Page<JournalResponse> response = queryService.listJournals(
                1L, 1L, 10L, JournalStatus.ACTIVE, LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30), "수학", pageable);

        assertThat(response.getContent()).isEmpty();
    }

    @Test
    @DisplayName("getJournalsByClass 성공 시 해당 수업의 수업일지 목록을 반환한다")
    void getJournalsByClass_success() throws Exception {
        ClassJournal journal = buildJournal(1L);
        Pageable pageable = PageRequest.of(0, 20);
        Page<ClassJournal> page = new PageImpl<>(List.of(journal), pageable, 1);

        given(journalRepository.findByClassId(1L, pageable)).willReturn(page);

        Page<JournalResponse> response = queryService.getJournalsByClass(1L, pageable);

        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getJournalsByTeacher 성공 시 해당 강사의 수업일지 목록을 반환한다")
    void getJournalsByTeacher_success() throws Exception {
        ClassJournal journal = buildJournal(1L);
        Pageable pageable = PageRequest.of(0, 20);
        Page<ClassJournal> page = new PageImpl<>(List.of(journal), pageable, 1);

        given(journalRepository.findByTeacherId(10L, pageable)).willReturn(page);

        Page<JournalResponse> response = queryService.getJournalsByTeacher(10L, pageable);

        assertThat(response.getContent()).hasSize(1);
    }

    // -- helpers --

    private ClassJournal buildJournal(Long id) throws Exception {
        ClassJournal journal = ClassJournal.create(
                1L, 1L, 10L, LocalDate.of(2026, 4, 1),
                "수학 수업일지", "미적분 기초", List.of("수학", "미적분"),
                "교재 23~25페이지", JournalVisibility.TEACHER_ONLY, null);
        setField(journal, "id", id);
        return journal;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
    }
}
