package com.lumie.auth.adapter.out.persistence;

import com.lumie.auth.api.UserIdentityChangedEvent;
import com.lumie.auth.domain.entity.User;
import com.lumie.common.auth.Role;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UserPersistenceAdapterIdentityTest {
    @Test void userWriteNormalizesPhoneAndPublishesCanonicalIdentity() {
        var repository = mock(UserJpaRepository.class);
        var events = mock(ApplicationEventPublisher.class);
        var user = User.create("user_123", "이름", "010-1111-2222", "hash", Role.STUDENT);
        when(repository.findById(7L)).thenReturn(Optional.of(user));
        when(repository.save(user)).thenReturn(user);
        var adapter = new UserPersistenceAdapter(repository, events);

        assertThat(adapter.updateIdentity(7L, "새 이름", "010-3333-4444", "new@example.com")).isTrue();
        assertThat(user.getPhone()).isEqualTo("01033334444");
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        verify(events).publishEvent(new UserIdentityChangedEvent(
                7L, "새 이름", "01033334444", "new@example.com", true, true, true));
    }

    @Test void nameOnlyWriteDoesNotProjectAnOldUserPhone() {
        var repository = mock(UserJpaRepository.class);
        var events = mock(ApplicationEventPublisher.class);
        var user = User.create("user_123", "옛 이름", "01011112222", "hash", Role.STUDENT);
        when(repository.findById(7L)).thenReturn(Optional.of(user));
        when(repository.save(user)).thenReturn(user);

        new UserPersistenceAdapter(repository, events).updateIdentity(7L, "새 이름", null, null);

        verify(events).publishEvent(new UserIdentityChangedEvent(
                7L, "새 이름", "01011112222", null, true, false, false));
    }

    @Test void identityBlankPhoneClearsStoredValueAndPublishesChange() {
        var repository = mock(UserJpaRepository.class);
        var events = mock(ApplicationEventPublisher.class);
        var user = User.create("user_123", "이름", "01011112222", "hash", Role.STUDENT);
        when(repository.findById(7L)).thenReturn(Optional.of(user));
        when(repository.save(user)).thenReturn(user);

        new UserPersistenceAdapter(repository, events).updateIdentity(7L, null, "", null);

        assertThat(user.getPhone()).isNull();
        verify(events).publishEvent(new UserIdentityChangedEvent(
                7L, "이름", null, null, false, true, false));
    }

    @Test void profileNullContactFieldsClearStoredValues() {
        var repository = mock(UserJpaRepository.class);
        var events = mock(ApplicationEventPublisher.class);
        var user = User.create("user_123", "옛 이름", "01011112222", "hash", Role.STUDENT);
        when(repository.findById(7L)).thenReturn(Optional.of(user));
        when(repository.save(user)).thenReturn(user);

        new UserPersistenceAdapter(repository, events).updateProfile(7L, "새 이름", null, null);

        assertThat(user.getPhone()).isNull();
        verify(events).publishEvent(new UserIdentityChangedEvent(
                7L, "새 이름", null, null, true, true, false));
    }

    @Test void profileBlankContactFieldsClearStoredValuesAndPublishChanges() {
        var repository = mock(UserJpaRepository.class);
        var events = mock(ApplicationEventPublisher.class);
        var user = User.create("user_123", "옛 이름", "01011112222", "hash", Role.STUDENT);
        user.updateProfile("옛 이름", "01011112222", "old@example.com");
        when(repository.findById(7L)).thenReturn(Optional.of(user));
        when(repository.save(user)).thenReturn(user);

        new UserPersistenceAdapter(repository, events).updateProfile(7L, "옛 이름", "", " ");

        assertThat(user.getPhone()).isNull();
        assertThat(user.getEmail()).isNull();
        verify(events).publishEvent(new UserIdentityChangedEvent(
                7L, "옛 이름", null, null, false, true, true));
    }
}
