package com.lumie.messaging.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lumie.common.domain.AbstractDomainEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public abstract class MemberEvent extends AbstractDomainEvent {

    private final Long memberId;
    private final String slug;
    private final String memberType;

    protected MemberEvent(Long memberId, String slug, String memberType) {
        super();
        this.memberId = memberId;
        this.slug = slug;
        this.memberType = memberType;
    }

    @JsonIgnore
    public String getRoutingKey() {
        return String.format("member.%s.%s",
            getEventType().toLowerCase().replace("event", "").replace("member", ""),
            slug);
    }
}
