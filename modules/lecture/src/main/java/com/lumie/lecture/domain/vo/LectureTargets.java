package com.lumie.lecture.domain.vo;

import java.util.List;

public final class LectureTargets {
    private LectureTargets() {}

public static List<Long> primaryFirst(List<Long> classIds, Long primaryClassId) {
    if (classIds.isEmpty()) {
        return primaryClassId == null ? List.of() : List.of(primaryClassId);
    }
    if (primaryClassId == null || !classIds.contains(primaryClassId)) {
        return classIds;
    }
    return java.util.stream.Stream.concat(
                    java.util.stream.Stream.of(primaryClassId),
                    classIds.stream().filter(id -> !id.equals(primaryClassId))
            )
            .toList();
}
}
