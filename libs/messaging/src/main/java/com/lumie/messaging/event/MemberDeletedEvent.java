package com.lumie.messaging.event;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class MemberDeletedEvent extends MemberEvent {

    public MemberDeletedEvent(Long memberId, String slug, String memberType) {
        super(memberId, slug, memberType);
    }

    @Override
    public String getRoutingKey() {
        return String.format("member.deleted.%s", getSlug());
    }
}
