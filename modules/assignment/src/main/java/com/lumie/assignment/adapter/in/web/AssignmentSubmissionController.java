package com.lumie.assignment.adapter.in.web;

import com.lumie.assignment.application.dto.request.CreateSubmissionRequest;
import com.lumie.assignment.application.dto.request.UpdateSubmissionRequest;
import com.lumie.assignment.application.dto.response.SubmissionResponse;
import com.lumie.assignment.application.service.AssignmentCommandService;
import com.lumie.assignment.application.service.AssignmentQueryService;
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

@RestController
@RequestMapping("/v1/assignments/{assignmentId}/submissions")
@RequiredArgsConstructor
public class AssignmentSubmissionController {

    private final AssignmentCommandService commandService;
    private final AssignmentQueryService queryService;

    @GetMapping
    @AllowedSortFields({"createdAt", "submittedAt", "status"})
    public ResponseEntity<PageResponse<SubmissionResponse>> getSubmissions(
            @PathVariable Long assignmentId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<SubmissionResponse> response = queryService.getSubmissions(assignmentId, pageable);
        return ResponseEntity.ok(PageResponse.from(response));
    }

    @PostMapping
    public ResponseEntity<SubmissionResponse> createSubmission(
            @PathVariable Long assignmentId,
            @Valid @RequestBody CreateSubmissionRequest request) {
        SubmissionResponse response = commandService.createSubmission(assignmentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{submissionId}")
    public ResponseEntity<SubmissionResponse> updateSubmission(
            @PathVariable Long assignmentId,
            @PathVariable Long submissionId,
            @Valid @RequestBody UpdateSubmissionRequest request) {
        SubmissionResponse response = commandService.updateSubmission(assignmentId, submissionId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/student/{studentId}")
    @AllowedSortFields({"createdAt", "submittedAt", "status"})
    public ResponseEntity<PageResponse<SubmissionResponse>> getStudentSubmissions(
            @PathVariable Long assignmentId,
            @PathVariable Long studentId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<SubmissionResponse> response = queryService.getStudentSubmissions(assignmentId, studentId, pageable);
        return ResponseEntity.ok(PageResponse.from(response));
    }
}
