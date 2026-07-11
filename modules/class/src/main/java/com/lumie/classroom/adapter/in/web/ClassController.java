package com.lumie.classroom.adapter.in.web;

import com.lumie.classroom.application.dto.ClassResponse;
import com.lumie.classroom.application.dto.CreateClassRequest;
import com.lumie.classroom.application.dto.UpdateClassRequest;
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


@RestController
@RequestMapping("/v1/classes")
@RequiredArgsConstructor
public class ClassController {

    private final ClassCommandService commandService;
    private final ClassQueryService queryService;

    @PostMapping
    public ResponseEntity<ClassResponse> createClass(
            @Valid @RequestBody CreateClassRequest request) {
        ClassResponse response = commandService.createClass(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @AllowedSortFields({"createdAt", "name"})
    public ResponseEntity<PageResponse<ClassResponse>> listClasses(
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ClassResponse> response = queryService.listClasses(teacherId, search, pageable);
        return ResponseEntity.ok(PageResponse.from(response));
    }

    @GetMapping("/me")
    @AllowedSortFields({"createdAt", "name"})
    public ResponseEntity<PageResponse<ClassResponse>> getClassesForCurrentStudent(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ClassResponse> response = queryService.getClassesForCurrentStudent(pageable);
        return ResponseEntity.ok(PageResponse.from(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClassResponse> getClass(@PathVariable Long id) {
        ClassResponse response = queryService.getClass(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ClassResponse> updateClass(
            @PathVariable Long id,
            @Valid @RequestBody UpdateClassRequest request) {
        ClassResponse response = commandService.updateClass(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClass(@PathVariable Long id) {
        commandService.deleteClass(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/teacher/{teacherId}")
    @AllowedSortFields({"createdAt", "name"})
    public ResponseEntity<PageResponse<ClassResponse>> getClassesByTeacher(
            @PathVariable Long teacherId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ClassResponse> response = queryService.getClassesByTeacher(teacherId, pageable);
        return ResponseEntity.ok(PageResponse.from(response));
    }

    @GetMapping("/student/{studentId}")
    @AllowedSortFields({"createdAt", "name"})
    public ResponseEntity<PageResponse<ClassResponse>> getClassesByStudent(
            @PathVariable Long studentId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ClassResponse> response = queryService.getClassesByStudent(studentId, pageable);
        return ResponseEntity.ok(PageResponse.from(response));
    }
}
