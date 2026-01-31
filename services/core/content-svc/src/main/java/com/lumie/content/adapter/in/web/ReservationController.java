package com.lumie.content.adapter.in.web;

import com.lumie.content.application.dto.request.CreateReservationRequest;
import com.lumie.content.application.dto.request.UpdateReservationStatusRequest;
import com.lumie.content.application.dto.response.ReservationResponse;
import com.lumie.content.application.service.ReservationCommandService;
import com.lumie.content.application.service.ReservationQueryService;
import com.lumie.content.domain.vo.ReservationStatus;
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
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationCommandService commandService;
    private final ReservationQueryService queryService;

    @GetMapping
    public ResponseEntity<Page<ReservationResponse>> listReservations(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(queryService.listReservations(pageable));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<ReservationResponse>> listReservationsByStudent(
            @PathVariable Long studentId,
            @RequestParam(required = false) ReservationStatus status) {
        if (status != null) {
            return ResponseEntity.ok(queryService.listReservationsByStudentAndStatus(studentId, status));
        }
        return ResponseEntity.ok(queryService.listReservationsByStudent(studentId));
    }

    @GetMapping("/schedule/{scheduleId}")
    public ResponseEntity<List<ReservationResponse>> listReservationsBySchedule(@PathVariable Long scheduleId) {
        return ResponseEntity.ok(queryService.listReservationsBySchedule(scheduleId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable Long id) {
        return ResponseEntity.ok(queryService.getReservation(id));
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody CreateReservationRequest request) {
        ReservationResponse response = commandService.createReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ReservationResponse> updateReservationStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReservationStatusRequest request) {
        return ResponseEntity.ok(commandService.updateReservationStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelReservation(@PathVariable Long id) {
        commandService.cancelReservation(id);
        return ResponseEntity.noContent().build();
    }
}
