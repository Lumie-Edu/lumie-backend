package com.lumie.content.adapter.in.web;

import com.lumie.content.application.dto.request.CreateTextbookRequest;
import com.lumie.content.application.dto.request.UpdateTextbookRequest;
import com.lumie.content.application.dto.response.TextbookResponse;
import com.lumie.content.application.service.TextbookCommandService;
import com.lumie.content.application.service.TextbookQueryService;
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
@RequestMapping("/api/v1/textbooks")
@RequiredArgsConstructor
public class TextbookController {

    private final TextbookCommandService commandService;
    private final TextbookQueryService queryService;

    @GetMapping
    public ResponseEntity<Page<TextbookResponse>> listTextbooks(
            @RequestParam(required = false) String subject,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        if (subject != null && !subject.isBlank()) {
            return ResponseEntity.ok(queryService.listTextbooksBySubject(subject, pageable));
        }
        return ResponseEntity.ok(queryService.listTextbooks(pageable));
    }

    @GetMapping("/active")
    public ResponseEntity<List<TextbookResponse>> listActiveTextbooks() {
        return ResponseEntity.ok(queryService.listActiveTextbooks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TextbookResponse> getTextbook(@PathVariable Long id) {
        return ResponseEntity.ok(queryService.getTextbook(id));
    }

    @PostMapping
    public ResponseEntity<TextbookResponse> createTextbook(@Valid @RequestBody CreateTextbookRequest request) {
        TextbookResponse response = commandService.createTextbook(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TextbookResponse> updateTextbook(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTextbookRequest request) {
        return ResponseEntity.ok(commandService.updateTextbook(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTextbook(@PathVariable Long id) {
        commandService.deleteTextbook(id);
        return ResponseEntity.noContent().build();
    }
}
