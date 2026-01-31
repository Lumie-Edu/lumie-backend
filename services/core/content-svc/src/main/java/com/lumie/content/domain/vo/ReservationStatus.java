package com.lumie.content.domain.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReservationStatus {

    PENDING("대기중"),
    CONFIRMED("확정"),
    CANCELLED("취소"),
    COMPLETED("완료");

    private final String displayName;
}
