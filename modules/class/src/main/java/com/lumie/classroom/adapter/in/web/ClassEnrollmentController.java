package com.lumie.classroom.adapter.in.web;

import com.lumie.classroom.application.dto.ClassEnrollmentResponse;
import com.lumie.classroom.application.dto.EnrollStudentsRequest;
import com.lumie.classroom.application.dto.UpdateEnrollmentRequest;
import com.lumie.classroom.application.service.ClassCommandService;
import com.lumie.classroom.application.service.ClassQueryService;
import com.lumie.common.api.AllowedSortFields;
import com.lumie.common.api.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/classes/{classId}/enrollments")
@RequiredArgsConstructor
public class ClassEnrollmentController {

    private final ClassCommandService commandService;
    private final ClassQueryService queryService;

    @GetMapping
    @AllowedSortFields({"enrolledAt", "createdAt", "status"})
    public ResponseEntity<PageResponse<ClassEnrollmentResponse>> getEnrollments(
            @PathVariable Long classId,
            @PageableDefault(size = 20, sort = "enrolledAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ClassEnrollmentResponse> response = queryService.getEnrollments(classId, pageable);
        return ResponseEntity.ok(PageResponse.from(response));
    }

    @PostMapping
    public ResponseEntity<List<ClassEnrollmentResponse>> enrollStudents(
            @PathVariable Long classId,
            @Valid @RequestBody EnrollStudentsRequest request) {
        List<ClassEnrollmentResponse> response = commandService.enrollStudents(classId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{enrollmentId}")
    public ResponseEntity<Void> removeEnrollment(
            @PathVariable Long classId,
            @PathVariable Long enrollmentId) {
        commandService.removeEnrollment(classId, enrollmentId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/student/{studentId}")
    public ResponseEntity<Void> removeEnrollmentByStudent(
            @PathVariable Long classId,
            @PathVariable Long studentId) {
        commandService.removeEnrollmentByStudent(classId, studentId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{enrollmentId}")
    public ResponseEntity<ClassEnrollmentResponse> updateEnrollmentStatus(
            @PathVariable Long classId,
            @PathVariable Long enrollmentId,
            @Valid @RequestBody UpdateEnrollmentRequest request) {
        ClassEnrollmentResponse response = commandService.updateEnrollmentStatus(classId, enrollmentId, request);
        return ResponseEntity.ok(response);
    }
}
