package com.lumie.notification.adapter.in.web;

import com.lumie.notification.application.dto.CreateTemplateRequest;
import com.lumie.notification.application.dto.SmsTemplateResponse;
import com.lumie.notification.application.dto.UpdateTemplateRequest;
import com.lumie.notification.application.service.SmsCommandService;
import com.lumie.notification.application.service.SmsQueryService;
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
@RequestMapping("/v1/sms/templates")
@RequiredArgsConstructor
public class SmsTemplateController {

    private final SmsCommandService commandService;
    private final SmsQueryService queryService;

    @GetMapping
    public ResponseEntity<Page<SmsTemplateResponse>> listTemplates(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<SmsTemplateResponse> response = queryService.listTemplates(pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<SmsTemplateResponse> createTemplate(
            @Valid @RequestBody CreateTemplateRequest request) {
        SmsTemplateResponse response = commandService.createTemplate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SmsTemplateResponse> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTemplateRequest request) {
        SmsTemplateResponse response = commandService.updateTemplate(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        commandService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }
}
