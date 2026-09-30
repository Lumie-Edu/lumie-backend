package com.lumie.staff.adapter.in.internal;

import com.lumie.auth.api.UserIdentityChangedEvent;
import com.lumie.staff.domain.entity.Staff;
import com.lumie.staff.domain.repository.StaffRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UserIdentityProjectionTest {
    @Test void canonicalUserChangeUpdatesLinkedProjection() {
        var repository = mock(StaffRepository.class);
        var entity = Staff.create(7L, "user_123", "기존", "01011112222", null, null);
        when(repository.findByUserId(7L)).thenReturn(Optional.of(entity));
        new UserIdentityProjection(repository).onUserIdentityChanged(
                new UserIdentityChangedEvent(7L, "새 이름", "010-3333-4444", "new@example.com", true, true, true));
        assertThat(entity.getStaffName()).isEqualTo("새 이름");
        assertThat(entity.getStaffPhone()).isEqualTo("01033334444");
        assertThat(entity.getEmail()).isEqualTo("new@example.com");
    }

    @Test void emailOnlyChangePreservesOtherIdentityFields() {
        var repository = mock(StaffRepository.class);
        var entity = Staff.create(7L, "user_123", "기존", "01099998888", "old@example.com", null);
        when(repository.findByUserId(7L)).thenReturn(Optional.of(entity));
        new UserIdentityProjection(repository).onUserIdentityChanged(
                new UserIdentityChangedEvent(7L, "다른 User 이름", "01011112222", "new@example.com", false, false, true));
        assertThat(entity.getStaffName()).isEqualTo("기존");
        assertThat(entity.getStaffPhone()).isEqualTo("01099998888");
        assertThat(entity.getEmail()).isEqualTo("new@example.com");
    }
}
