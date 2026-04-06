package com.lumie.cls.domain.vo;

public record ClassSearchCriteria(
    Long academyId,
    Long teacherId,
    String search
) {
}
