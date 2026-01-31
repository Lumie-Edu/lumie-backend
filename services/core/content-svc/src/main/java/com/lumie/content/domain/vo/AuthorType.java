package com.lumie.content.domain.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthorType {

    STUDENT("학생"),
    ADMIN("관리자");

    private final String displayName;
}
