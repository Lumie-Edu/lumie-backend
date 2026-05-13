package com.lumie.lecture.adapter.in.web;

import com.lumie.lecture.application.dto.LectureResponse;
import com.lumie.lecture.application.dto.WatchResponse;
import com.lumie.lecture.application.service.LectureQueryService;
import com.lumie.common.api.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/lectures/student")
@RequiredArgsConstructor
public class LectureStudentController {

    private final LectureQueryService queryService;

    @GetMapping("/class/{classId}")
    public ResponseEntity<PageResponse<LectureResponse>> findByClass(
            @PathVariable Long classId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(queryService.findByClassIdForStudent(classId, pageable)));
    }

    @GetMapping("/{id}/watch")
    public ResponseEntity<WatchResponse> getWatchUrls(@PathVariable Long id) {
        return ResponseEntity.ok(queryService.getWatchUrls(id));
    }
}
