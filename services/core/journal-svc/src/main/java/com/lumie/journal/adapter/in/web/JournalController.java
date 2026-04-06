package com.lumie.journal.adapter.in.web;

import com.lumie.journal.application.dto.CreateJournalRequest;
import com.lumie.journal.application.dto.JournalResponse;
import com.lumie.journal.application.dto.UpdateJournalRequest;
import com.lumie.journal.application.service.JournalCommandService;
import com.lumie.journal.application.service.JournalQueryService;
import com.lumie.journal.domain.vo.JournalStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/v1/journals")
@RequiredArgsConstructor
public class JournalController {

    private final JournalCommandService commandService;
    private final JournalQueryService queryService;

    @PostMapping
    public ResponseEntity<JournalResponse> createJournal(
            @Valid @RequestBody CreateJournalRequest request) {
        JournalResponse response = commandService.createJournal(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<JournalResponse>> listJournals(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long academyId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) JournalStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "lessonDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<JournalResponse> response = queryService.listJournals(
            classId, academyId, teacherId, status, startDate, endDate, search, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JournalResponse> getJournal(@PathVariable Long id) {
        JournalResponse response = queryService.getJournal(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<JournalResponse> updateJournal(
            @PathVariable Long id,
            @Valid @RequestBody UpdateJournalRequest request) {
        JournalResponse response = commandService.updateJournal(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJournal(@PathVariable Long id) {
        commandService.deleteJournal(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<Page<JournalResponse>> getJournalsByClass(
            @PathVariable Long classId,
            @PageableDefault(size = 20, sort = "lessonDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<JournalResponse> response = queryService.getJournalsByClass(classId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<Page<JournalResponse>> getJournalsByTeacher(
            @PathVariable Long teacherId,
            @PageableDefault(size = 20, sort = "lessonDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<JournalResponse> response = queryService.getJournalsByTeacher(teacherId, pageable);
        return ResponseEntity.ok(response);
    }
}
