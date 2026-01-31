package com.lumie.academy.adapter.in.web;

import com.lumie.academy.application.dto.AcademyRequest;
import com.lumie.academy.application.dto.AcademyResponse;
import com.lumie.academy.application.dto.StudentResponse;
import com.lumie.academy.application.service.AcademyCommandService;
import com.lumie.academy.application.service.AcademyQueryService;
import com.lumie.academy.application.service.StudentQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/academies")
@RequiredArgsConstructor
public class AcademyController {

    private final AcademyCommandService academyCommandService;
    private final AcademyQueryService academyQueryService;
    private final StudentQueryService studentQueryService;

    @PostMapping
    public ResponseEntity<AcademyResponse> createAcademy(@Valid @RequestBody AcademyRequest request) {
        AcademyResponse response = academyCommandService.createAcademy(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AcademyResponse> getAcademy(@PathVariable Long id) {
        AcademyResponse response = academyQueryService.getAcademy(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<AcademyResponse>> getAllAcademies(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AcademyResponse> response = academyQueryService.getAllAcademies(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    public ResponseEntity<Page<AcademyResponse>> getActiveAcademies(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AcademyResponse> response = academyQueryService.getActiveAcademies(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/default")
    public ResponseEntity<AcademyResponse> getDefaultAcademy() {
        AcademyResponse response = academyQueryService.getDefaultAcademy();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AcademyResponse> updateAcademy(
            @PathVariable Long id,
            @Valid @RequestBody AcademyRequest request) {
        AcademyResponse response = academyCommandService.updateAcademy(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateAcademy(@PathVariable Long id) {
        academyCommandService.deactivateAcademy(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/students")
    public ResponseEntity<Page<StudentResponse>> getStudentsByAcademy(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<StudentResponse> response = studentQueryService.getActiveStudentsByAcademy(id, pageable);
        return ResponseEntity.ok(response);
    }
}
