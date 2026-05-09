package com.lumie.attendance.application.service;

import com.lumie.attendance.application.dto.AttendanceSessionRequest;
import com.lumie.attendance.application.dto.AttendanceSessionResponse;
import com.lumie.attendance.domain.entity.AttendanceRecord;
import com.lumie.attendance.domain.entity.AttendanceSession;
import com.lumie.attendance.domain.entity.StudentReadModel;
import com.lumie.attendance.domain.exception.SessionNotFoundException;
import com.lumie.attendance.domain.repository.AttendanceRecordRepository;
import com.lumie.attendance.domain.repository.AttendanceSessionRepository;
import com.lumie.attendance.domain.repository.StudentReadRepository;
import com.lumie.internal.cls.ClassService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
@DisplayName("AttendanceSessionCommandService 단위 테스트")
class AttendanceSessionCommandServiceTest {

    @Mock
    private AttendanceSessionRepository sessionRepository;

    @Mock
    private AttendanceRecordRepository recordRepository;

    @Mock
    private StudentReadRepository studentReadRepository;

    @Mock
    private ClassService classService;

    @InjectMocks
    private AttendanceSessionCommandService commandService;

    private AttendanceSessionRequest buildRequest() {
        return new AttendanceSessionRequest("수학 수업", LocalDate.now(), null, 10);
    }

    private AttendanceSession buildSavedSession(Long id) throws Exception {
        AttendanceSession session = AttendanceSession.create("수학 수업", LocalDate.now(), null, null, 10);
        setField(session, "id", id);
        return session;
    }

    private StudentReadModel buildStudent(Long id) throws Exception {
        StudentReadModel student = new StudentReadModel() {};
        setField(student, "id", id);
        setField(student, "name", "학생" + id);
        setField(student, "isActive", true);
        setField(student, "userId", id + 100L);
        return student;
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
        throw new NoSuchFieldException(fieldName + " not found in hierarchy of " + target.getClass());
    }

    @Test
    @DisplayName("createSession() 호출 시 세션이 저장되고 활성 학생 레코드가 생성된다")
    void createSession_savesSessionAndCreatesRecords() throws Exception {
        AttendanceSessionRequest request = buildRequest();
        AttendanceSession saved = buildSavedSession(1L);
        StudentReadModel s1 = buildStudent(10L);
        StudentReadModel s2 = buildStudent(11L);

        given(sessionRepository.save(any(AttendanceSession.class))).willReturn(saved);
        given(studentReadRepository.findByIsActiveTrue()).willReturn(List.of(s1, s2));
        given(recordRepository.saveAll(any())).willAnswer(inv -> inv.getArgument(0));

        AttendanceSessionResponse response = commandService.createSession(request);

        assertThat(response).isNotNull();

        ArgumentCaptor<List<AttendanceRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(recordRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    @DisplayName("createSession() 호출 시 활성 학생이 없으면 레코드가 생성되지 않는다")
    void createSession_noActiveStudents_savesEmptyRecords() throws Exception {
        AttendanceSession saved = buildSavedSession(1L);
        given(sessionRepository.save(any())).willReturn(saved);
        given(studentReadRepository.findByIsActiveTrue()).willReturn(List.of());
        given(recordRepository.saveAll(any())).willAnswer(inv -> inv.getArgument(0));

        commandService.createSession(buildRequest());

        ArgumentCaptor<List<AttendanceRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(recordRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    @DisplayName("regenerateCode() 호출 시 새 코드로 교체된 응답을 반환한다")
    void regenerateCode_returnsUpdatedSession() throws Exception {
        AttendanceSession session = buildSavedSession(1L);
        given(sessionRepository.findById(1L)).willReturn(Optional.of(session));
        given(sessionRepository.save(session)).willReturn(session);

        AttendanceSessionResponse response = commandService.regenerateCode(1L);

        assertThat(response).isNotNull();
        verify(sessionRepository).save(session);
    }

    @Test
    @DisplayName("regenerateCode() 호출 시 세션이 없으면 SessionNotFoundException을 던진다")
    void regenerateCode_notFound_throwsSessionNotFoundException() {
        given(sessionRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commandService.regenerateCode(99L))
            .isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    @DisplayName("deleteSession() 호출 시 세션이 삭제된다")
    void deleteSession_deletesSession() throws Exception {
        AttendanceSession session = buildSavedSession(1L);
        given(sessionRepository.findById(1L)).willReturn(Optional.of(session));

        commandService.deleteSession(1L);

        verify(sessionRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteSession() 호출 시 세션이 없으면 SessionNotFoundException을 던진다")
    void deleteSession_notFound_throwsSessionNotFoundException() {
        given(sessionRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commandService.deleteSession(99L))
            .isInstanceOf(SessionNotFoundException.class);
    }
}
