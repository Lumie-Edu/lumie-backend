package com.lumie.cls.domain.vo;

public record ClassSearchCriteria(
    Long academyId,
    Long teacherId,
    ClassStatus status,
    String search
) {
}
