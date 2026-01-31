package com.lumie.content.domain.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TextbookCategory {

    LISTENING("듣기자료"),
    MATERIAL("학습자료"),
    WEEKLY_TEST("주간테스트"),
    PPT("PPT"),
    ETC("기타"),
    ASSISTANT("보조자료");

    private final String displayName;
}
