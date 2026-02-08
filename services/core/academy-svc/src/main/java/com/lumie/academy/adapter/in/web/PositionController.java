package com.lumie.academy.adapter.in.web;

import com.lumie.academy.application.dto.PositionRequest;
import com.lumie.academy.application.dto.PositionResponse;
import com.lumie.academy.application.service.PositionCommandService;
import com.lumie.academy.application.service.PositionQueryService;
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
@RequestMapping("/api/v1/positions")
@RequiredArgsConstructor
public class PositionController {

    private final PositionCommandService positionCommandService;
    private final PositionQueryService positionQueryService;

    @PostMapping
    public ResponseEntity<PositionResponse> createPosition(@Valid @RequestBody PositionRequest request) {
        PositionResponse response = positionCommandService.createPosition(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PositionResponse> getPosition(@PathVariable Long id) {
        PositionResponse response = positionQueryService.getPosition(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<PositionResponse>> getAllPositions(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PositionResponse> response = positionQueryService.getAllPositions(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    public ResponseEntity<List<PositionResponse>> getActivePositions() {
        List<PositionResponse> response = positionQueryService.getActivePositions();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PositionResponse> updatePosition(
            @PathVariable Long id,
            @Valid @RequestBody PositionRequest request) {
        PositionResponse response = positionCommandService.updatePosition(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivatePosition(@PathVariable Long id) {
        positionCommandService.deactivatePosition(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<Void> reactivatePosition(@PathVariable Long id) {
        positionCommandService.reactivatePosition(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePosition(@PathVariable Long id) {
        positionCommandService.deletePosition(id);
        return ResponseEntity.noContent().build();
    }
}
