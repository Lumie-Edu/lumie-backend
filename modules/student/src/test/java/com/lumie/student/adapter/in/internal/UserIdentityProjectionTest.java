package com.lumie.student.adapter.in.internal;

import com.lumie.auth.api.UserIdentityChangedEvent;
import com.lumie.student.domain.entity.Student;
import com.lumie.student.domain.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UserIdentityProjectionTest {
    @Test void canonicalUserChangeUpdatesLinkedProjection() {
        var repository = mock(StudentRepository.class);
        var entity = Student.create(7L, "user_123", "기존", "01011112222", null, null, null, null);
        when(repository.findByUserId(7L)).thenReturn(Optional.of(entity));
        new UserIdentityProjection(repository).onUserIdentityChanged(
                new UserIdentityChangedEvent(7L, "새 이름", "010-3333-4444", "new@example.com", true, true, true));
        assertThat(entity.getStudentName()).isEqualTo("새 이름");
        assertThat(entity.getStudentPhone()).isEqualTo("01033334444");
    }

    @Test void nameOnlyChangePreservesUnresolvedPhoneConflict() {
        var repository = mock(StudentRepository.class);
        var entity = Student.create(7L, "user_123", "기존", "01099998888", null, null, null, null);
        // Simulate a loaded entity: User is the displayed source while the old column
        // remains unchanged for rolling compatibility until the contract release.
        org.springframework.test.util.ReflectionTestUtils.setField(entity, "canonicalName", "기존");
        org.springframework.test.util.ReflectionTestUtils.setField(entity, "canonicalPhone", "01011112222");
        when(repository.findByUserId(7L)).thenReturn(Optional.of(entity));

        new UserIdentityProjection(repository).onUserIdentityChanged(
                new UserIdentityChangedEvent(7L, "새 이름", "01011112222", null, true, false, false));

        assertThat(entity.getStudentName()).isEqualTo("새 이름");
        assertThat(entity.getStudentPhone()).isEqualTo("01011112222");
        assertThat(org.springframework.test.util.ReflectionTestUtils.getField(entity, "phone"))
                .isEqualTo("01099998888");
    }

    @Test void emailOnlyChangeDoesNotLoadStudent() {
        var repository = mock(StudentRepository.class);
        new UserIdentityProjection(repository).onUserIdentityChanged(
                new UserIdentityChangedEvent(7L, "기존", "01011112222", "new@example.com", false, false, true));
        verifyNoInteractions(repository);
    }
}
