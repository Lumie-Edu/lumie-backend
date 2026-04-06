package com.lumie.journal.application.service;

import com.lumie.journal.application.dto.CreateJournalRequest;
import com.lumie.journal.application.dto.JournalResponse;
import com.lumie.journal.application.dto.UpdateJournalRequest;
import com.lumie.journal.application.port.out.AcademyServicePort;
import com.lumie.journal.application.port.out.AdminServicePort;
import com.lumie.journal.application.port.out.ClassServicePort;
import com.lumie.journal.domain.entity.ClassJournal;
import com.lumie.journal.domain.exception.JournalNotFoundException;
import com.lumie.journal.domain.repository.JournalRepository;
import com.lumie.journal.domain.vo.JournalStatus;
import com.lumie.journal.domain.vo.JournalVisibility;
import com.lumie.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("JournalCommandService 테스트")
class JournalCommandServiceTest {

    @Mock private JournalRepository journalRepository;
    @Mock private ClassServicePort classServicePort;
    @Mock private AcademyServicePort academyServicePort;
    @Mock private AdminServicePort adminServicePort;
    @InjectMocks private JournalCommandService commandService;

    @Nested
    @DisplayName("createJournal")
    class CreateJournal {

        @Test
        @DisplayName("정상 생성 시 JournalResponse를 반환한다")
        void createJournal_success() {
            CreateJournalRequest request = new CreateJournalRequest(
                    1L, 1L, 10L, LocalDate.of(2026, 4, 1),
                    "수학 수업일지", "미적분 기초", List.of("수학"), "숙제 없음",
                    "TEACHER_ONLY", null);

            given(classServicePort.getClass(1L))
                    .willReturn(Optional.of(new ClassServicePort.ClassInfo(1L, "수학반", 1L)));
            given(academyServicePort.getAcademy(1L))
                    .willReturn(Optional.of(new AcademyServicePort.AcademyInfo(1L, "테스트학원", true)));
            given(journalRepository.existsByClassIdAndLessonDate(1L, LocalDate.of(2026, 4, 1)))
                    .willReturn(false);
            given(journalRepository.save(any(ClassJournal.class)))
                    .willAnswer(inv -> {
                        ClassJournal journal = inv.getArgument(0);
                        setField(journal, "id", 1L);
                        return journal;
                    });

            JournalResponse response = commandService.createJournal(request);

            assertThat(response.title()).isEqualTo("수학 수업일지");
            assertThat(response.status()).isEqualTo(JournalStatus.ACTIVE);
            assertThat(response.visibility()).isEqualTo(JournalVisibility.TEACHER_ONLY);
        }

        @Test
        @DisplayName("존재하지 않는 수업으로 생성 시 BusinessException이 발생한다")
        void createJournal_classNotFound_throwsException() {
            CreateJournalRequest request = new CreateJournalRequest(
                    999L, 1L, 10L, LocalDate.of(2026, 4, 1),
                    "수업일지", null, null, null, null, null);

            given(classServicePort.getClass(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> commandService.createJournal(request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("존재하지 않는 학원으로 생성 시 BusinessException이 발생한다")
        void createJournal_academyNotFound_throwsException() {
            CreateJournalRequest request = new CreateJournalRequest(
                    1L, 999L, 10L, LocalDate.of(2026, 4, 1),
                    "수업일지", null, null, null, null, null);

            given(classServicePort.getClass(1L))
                    .willReturn(Optional.of(new ClassServicePort.ClassInfo(1L, "수학반", 1L)));
            given(academyServicePort.getAcademy(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> commandService.createJournal(request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("비활성 학원으로 생성 시 BusinessException이 발생한다")
        void createJournal_inactiveAcademy_throwsException() {
            CreateJournalRequest request = new CreateJournalRequest(
                    1L, 1L, 10L, LocalDate.of(2026, 4, 1),
                    "수업일지", null, null, null, null, null);

            given(classServicePort.getClass(1L))
                    .willReturn(Optional.of(new ClassServicePort.ClassInfo(1L, "수학반", 1L)));
            given(academyServicePort.getAcademy(1L))
                    .willReturn(Optional.of(new AcademyServicePort.AcademyInfo(1L, "학원", false)));

            assertThatThrownBy(() -> commandService.createJournal(request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("같은 날짜에 중복 수업일지 생성 시 BusinessException이 발생한다")
        void createJournal_duplicateDate_throwsException() {
            CreateJournalRequest request = new CreateJournalRequest(
                    1L, 1L, 10L, LocalDate.of(2026, 4, 1),
                    "수업일지", null, null, null, null, null);

            given(classServicePort.getClass(1L))
                    .willReturn(Optional.of(new ClassServicePort.ClassInfo(1L, "수학반", 1L)));
            given(academyServicePort.getAcademy(1L))
                    .willReturn(Optional.of(new AcademyServicePort.AcademyInfo(1L, "학원", true)));
            given(journalRepository.existsByClassIdAndLessonDate(1L, LocalDate.of(2026, 4, 1)))
                    .willReturn(true);

            assertThatThrownBy(() -> commandService.createJournal(request))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("updateJournal")
    class UpdateJournal {

        @Test
        @DisplayName("정상 업데이트 시 변경된 JournalResponse를 반환한다")
        void updateJournal_success() throws Exception {
            ClassJournal journal = buildJournal(1L);
            UpdateJournalRequest request = new UpdateJournalRequest(
                    "변경된 제목", null, null, null, null, "ACTIVE", null, null);

            given(journalRepository.findById(1L)).willReturn(Optional.of(journal));
            given(journalRepository.save(any(ClassJournal.class))).willAnswer(inv -> inv.getArgument(0));

            JournalResponse response = commandService.updateJournal(1L, request);

            assertThat(response.title()).isEqualTo("변경된 제목");
            assertThat(response.status()).isEqualTo(JournalStatus.ACTIVE);
        }

        @Test
        @DisplayName("잘못된 상태값으로 업데이트 시 BusinessException이 발생한다")
        void updateJournal_invalidStatus_throwsException() throws Exception {
            ClassJournal journal = buildJournal(1L);
            UpdateJournalRequest request = new UpdateJournalRequest(
                    null, null, null, null, null, "INVALID", null, null);

            given(journalRepository.findById(1L)).willReturn(Optional.of(journal));

            assertThatThrownBy(() -> commandService.updateJournal(1L, request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("존재하지 않는 수업일지 업데이트 시 JournalNotFoundException이 발생한다")
        void updateJournal_notFound_throwsException() {
            given(journalRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> commandService.updateJournal(999L,
                    new UpdateJournalRequest(null, null, null, null, null, null, null, null)))
                    .isInstanceOf(JournalNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteJournal")
    class DeleteJournal {

        @Test
        @DisplayName("삭제 시 상태가 CLOSED로 변경된다 (소프트 삭제)")
        void deleteJournal_softDeletes() throws Exception {
            ClassJournal journal = buildJournal(1L);
            given(journalRepository.findById(1L)).willReturn(Optional.of(journal));
            given(journalRepository.save(any(ClassJournal.class))).willAnswer(inv -> inv.getArgument(0));

            commandService.deleteJournal(1L);

            assertThat(journal.getStatus()).isEqualTo(JournalStatus.CLOSED);
            verify(journalRepository).save(journal);
        }
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
