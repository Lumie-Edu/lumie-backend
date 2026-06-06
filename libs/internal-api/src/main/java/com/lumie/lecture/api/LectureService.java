package com.lumie.lecture.api;

import java.util.List;
import java.util.Optional;

public interface LectureService {

    Optional<LectureData> getLecture(String tenantSlug, Long lectureId);

    LectureListData getLecturesByClass(String tenantSlug, Long classId, int page, int size);

    record LectureData(
            Long id,
            Long classId,
            List<Long> classIds,
            Long teacherId,
            String title,
            String description,
            String visibility,
            String createdAt,
            String updatedAt
    ) {}

    record LectureListData(
            List<LectureData> lectures,
            long totalCount,
            int page,
            int size
    ) {}
}
