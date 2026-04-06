package com.lumie.assignment.adapter.in.web;

import com.lumie.assignment.application.dto.AssignmentResponse;
import com.lumie.assignment.application.dto.CreateAssignmentRequest;
import com.lumie.assignment.application.dto.UpdateAssignmentRequest;
import com.lumie.assignment.application.service.AssignmentCommandService;
import com.lumie.assignment.application.service.AssignmentQueryService;
import com.lumie.assignment.domain.vo.AssignmentStatus;
import com.lumie.assignment.domain.vo.AssignmentType;
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
@RequestMapping("/v1/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentCommandService commandService;
    private final AssignmentQueryService queryService;

    @PostMapping
    public ResponseEntity<AssignmentResponse> createAssignment(
            @Valid @RequestBody CreateAssignmentRequest request) {
        AssignmentResponse response = commandService.createAssignment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<AssignmentResponse>> listAssignments(
            @RequestParam(required = false) Long academyId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) AssignmentType type,
            @RequestParam(required = false) AssignmentStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AssignmentResponse> response = queryService.listAssignments(academyId, classId, type, status, search, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssignmentResponse> getAssignment(@PathVariable Long id) {
        AssignmentResponse response = queryService.getAssignment(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AssignmentResponse> updateAssignment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAssignmentRequest request) {
        AssignmentResponse response = commandService.updateAssignment(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAssignment(@PathVariable Long id) {
        commandService.deleteAssignment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<Page<AssignmentResponse>> getAssignmentsByClass(
            @PathVariable Long classId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AssignmentResponse> response = queryService.getAssignmentsByClass(classId, pageable);
        return ResponseEntity.ok(response);
    }
}
