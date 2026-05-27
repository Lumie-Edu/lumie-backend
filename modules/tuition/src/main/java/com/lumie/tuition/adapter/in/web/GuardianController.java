package com.lumie.tuition.adapter.in.web;

import com.lumie.common.api.AllowedSortFields;
import com.lumie.common.api.PageResponse;
import com.lumie.tuition.application.dto.request.CreateGuardianRequest;
import com.lumie.tuition.application.dto.request.UpdateGuardianRequest;
import com.lumie.tuition.application.dto.response.GuardianResponse;
import com.lumie.tuition.application.service.GuardianCommandService;
import com.lumie.tuition.application.service.GuardianQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/guardians")
@RequiredArgsConstructor
public class GuardianController {

    private final GuardianCommandService commandService;
    private final GuardianQueryService queryService;

    @PostMapping
    public ResponseEntity<GuardianResponse> createGuardian(
            @Valid @RequestBody CreateGuardianRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commandService.createGuardian(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GuardianResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(queryService.getById(id));
    }

    @GetMapping
    @AllowedSortFields({"createdAt", "name", "phone"})
    public ResponseEntity<PageResponse<GuardianResponse>> listGuardians(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(queryService.listAll(pageable)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<GuardianResponse> updateGuardian(
            @PathVariable Long id,
            @Valid @RequestBody UpdateGuardianRequest request) {
        return ResponseEntity.ok(commandService.updateGuardian(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGuardian(@PathVariable Long id) {
        commandService.deleteGuardian(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{guardianId}/link")
    public ResponseEntity<Void> linkToStudent(
            @PathVariable Long guardianId,
            @RequestParam Long studentId,
            @RequestParam(defaultValue = "false") boolean isPrimary) {
        commandService.linkGuardianToStudent(studentId, guardianId, isPrimary);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{guardianId}/link")
    public ResponseEntity<Void> unlinkFromStudent(
            @PathVariable Long guardianId,
            @RequestParam Long studentId) {
        commandService.unlinkGuardianFromStudent(studentId, guardianId);
        return ResponseEntity.noContent().build();
    }
}
