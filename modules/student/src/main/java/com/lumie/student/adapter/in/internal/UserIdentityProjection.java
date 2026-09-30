package com.lumie.student.adapter.in.internal;

import com.lumie.auth.api.UserIdentityChangedEvent;
import com.lumie.student.domain.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.stereotype.Component;

@Component("studentUserIdentityProjection")
@RequiredArgsConstructor
public class UserIdentityProjection {
    private final StudentRepository repository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onUserIdentityChanged(UserIdentityChangedEvent event) {
        if (!event.nameChanged() && !event.phoneChanged()) return;
        repository.findByUserId(event.userId()).ifPresent(person ->
                person.synchronizeIdentity(event.name(), event.phone(),
                        event.nameChanged(), event.phoneChanged()));
    }
}
