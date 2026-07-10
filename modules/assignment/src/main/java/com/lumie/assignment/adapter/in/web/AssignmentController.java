package com.lumie.assignment.adapter.in.web;

import com.lumie.assignment.application.dto.request.CreateAssignmentRequest;
import com.lumie.assignment.application.dto.request.SubmitAssignmentRequest;
import com.lumie.assignment.application.dto.request.UpdateAssignmentRequest;
import com.lumie.assignment.application.dto.response.AssignmentResponse;
import com.lumie.assignment.application.dto.response.AssignmentSubmissionResultResponse;
import com.lumie.assignment.application.dto.response.StudentAssignmentDetailResponse;
import com.lumie.assignment.application.service.AssignmentCommandService;
import com.lumie.assignment.application.service.AssignmentQueryService;
import com.lumie.assignment.domain.vo.AssignmentStatus;
import com.lumie.common.api.AllowedSortFields;
import com.lumie.common.api.PageResponse;
import com.lumie.common.auth.AuthorizationGuard;
import com.lumie.common.auth.Role;
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
@RequestMapping("/v1/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentCommandService commandService;
    private final AssignmentQueryService queryService;

    @PostMapping
    public ResponseEntity<AssignmentResponse> createAssignment(
            @Valid @RequestBody CreateAssignmentRequest request) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        AssignmentResponse response = commandService.createAssignment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @AllowedSortFields({"createdAt", "dueDate", "status"})
    public ResponseEntity<PageResponse<AssignmentResponse>> listAssignments(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) AssignmentStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        Page<AssignmentResponse> response = queryService.listAssignments(classId, status, pageable);
        return ResponseEntity.ok(PageResponse.from(response));
    }

    @GetMapping("/me")
    public ResponseEntity<List<AssignmentResponse>> listMyAssignments() {
        return ResponseEntity.ok(queryService.getCurrentStudentAssignments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssignmentResponse> getAssignment(@PathVariable Long id) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        AssignmentResponse response = queryService.getAssignment(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/me")
    public ResponseEntity<StudentAssignmentDetailResponse> getMyAssignment(@PathVariable Long id) {
        return ResponseEntity.ok(queryService.getCurrentStudentAssignment(id));
    }

    @PutMapping("/{id}/me/submission")
    public ResponseEntity<AssignmentSubmissionResultResponse> submitMyAssignment(
            @PathVariable Long id,
            @Valid @RequestBody SubmitAssignmentRequest request) {
        return ResponseEntity.ok(commandService.submitCurrentStudent(id, request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AssignmentResponse> updateAssignment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAssignmentRequest request) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        AssignmentResponse response = commandService.updateAssignment(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAssignment(@PathVariable Long id) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        commandService.deleteAssignment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/class/{classId}")
    @AllowedSortFields({"createdAt", "dueDate", "status"})
    public ResponseEntity<PageResponse<AssignmentResponse>> getAssignmentsByClass(
            @PathVariable Long classId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        Page<AssignmentResponse> response = queryService.getAssignmentsByClass(classId, pageable);
        return ResponseEntity.ok(PageResponse.from(response));
    }
}
