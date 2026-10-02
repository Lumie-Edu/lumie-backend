package com.lumie.lecture.domain.vo;

import java.util.List;

public final class LectureTargets {
    private LectureTargets() {}

    public static List<Long> canonical(List<Long> classIds) {
        return classIds.stream().distinct().sorted().toList();
    }
}
