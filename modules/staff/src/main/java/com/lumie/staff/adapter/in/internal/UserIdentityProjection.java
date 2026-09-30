package com.lumie.staff.adapter.in.internal;

import com.lumie.auth.api.UserIdentityChangedEvent;
import com.lumie.staff.domain.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.stereotype.Component;

@Component("staffUserIdentityProjection")
@RequiredArgsConstructor
public class UserIdentityProjection {
    private final StaffRepository repository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onUserIdentityChanged(UserIdentityChangedEvent event) {
        repository.findByUserId(event.userId()).ifPresent(person ->
                person.synchronizeIdentity(event.name(), event.phone(), event.email(),
                        event.nameChanged(), event.phoneChanged(), event.emailChanged()));
    }
}
