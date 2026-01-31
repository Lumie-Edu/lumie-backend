package com.lumie.messaging.event;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class MemberCreatedEvent extends MemberEvent {

    private final String memberName;
    private final String userId;
    private final Long academyId;

    public MemberCreatedEvent(Long memberId, String slug, String memberType,
                              String memberName, String userId, Long academyId) {
        super(memberId, slug, memberType);
        this.memberName = memberName;
        this.userId = userId;
        this.academyId = academyId;
    }

    @Override
    public String getRoutingKey() {
        return String.format("member.created.%s", getSlug());
    }
}
