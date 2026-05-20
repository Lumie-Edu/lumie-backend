package com.lumie.lecture.adapter.in.web;

import com.lumie.lecture.application.dto.request.CreateLectureRequest;
import com.lumie.lecture.application.dto.request.UpdateLectureRequest;
import com.lumie.lecture.application.dto.response.LectureResponse;
import com.lumie.lecture.application.service.LectureCommandService;
import com.lumie.lecture.application.service.LectureQueryService;
import com.lumie.common.api.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/v1/lectures")
@RequiredArgsConstructor
public class LectureController {

    private final LectureCommandService commandService;
    private final LectureQueryService queryService;

    @PostMapping
    public ResponseEntity<LectureResponse> create(@Valid @RequestBody CreateLectureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commandService.create(request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<LectureResponse>> findAll(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(queryService.findAll(
                classId, teacherId, status,
                startDate, endDate, search, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LectureResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(queryService.findById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<LectureResponse> update(@PathVariable Long id,
                                                    @RequestBody UpdateLectureRequest request) {
        return ResponseEntity.ok(commandService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        commandService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
