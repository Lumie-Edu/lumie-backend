package com.lumie.lecture.application.dto.response;

import com.lumie.lecture.domain.vo.LectureVisibility;
import io.swagger.v3.oas.annotations.media.Schema;

public record WatchResponse(
        Long lectureId,
        String title,
        @Schema(nullable = true) String description,
        @Schema(nullable = true) String youtubeUrl,
        LectureVisibility visibility
) {}
