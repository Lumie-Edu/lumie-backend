package com.lumie.messaging.event;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class MemberUpdatedEvent extends MemberEvent {

    private final String memberName;

    public MemberUpdatedEvent(Long memberId, String slug, String memberType, String memberName) {
        super(memberId, slug, memberType);
        this.memberName = memberName;
    }

    @Override
    public String getRoutingKey() {
        return String.format("member.updated.%s", getSlug());
    }
}
