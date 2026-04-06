package com.lumie.cls.adapter.in.web;

import com.lumie.cls.application.dto.ClassResponse;
import com.lumie.cls.application.dto.CreateClassRequest;
import com.lumie.cls.application.dto.UpdateClassRequest;
import com.lumie.cls.application.service.ClassCommandService;
import com.lumie.cls.application.service.ClassQueryService;
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
    public ResponseEntity<Page<ClassResponse>> listClasses(
            @RequestParam(required = false) Long academyId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ClassResponse> response = queryService.listClasses(academyId, teacherId, search, pageable);
        return ResponseEntity.ok(response);
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
    public ResponseEntity<Page<ClassResponse>> getClassesByTeacher(
            @PathVariable Long teacherId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ClassResponse> response = queryService.getClassesByTeacher(teacherId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<Page<ClassResponse>> getClassesByStudent(
            @PathVariable Long studentId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ClassResponse> response = queryService.getClassesByStudent(studentId, pageable);
        return ResponseEntity.ok(response);
    }
}
