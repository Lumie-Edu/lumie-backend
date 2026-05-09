package com.lumie.internal.lecture;

import java.util.List;
import java.util.Optional;

public interface LectureService {

    Optional<LectureData> getLecture(String tenantSlug, Long lectureId);

    LectureListData getLecturesByClass(String tenantSlug, Long classId, int page, int size);

    record LectureData(
            Long id,
            Long classId,
            Long teacherId,
            String title,
            String description,
            String visibility,
            String status,
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
