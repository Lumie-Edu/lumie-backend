package com.lumie.admin.application.port.out;

import com.lumie.messaging.event.MemberEvent;

public interface MemberEventPublisherPort {
    void publish(MemberEvent event);
}
