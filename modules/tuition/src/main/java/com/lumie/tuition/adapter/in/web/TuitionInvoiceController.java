package com.lumie.tuition.adapter.in.web;

import com.lumie.common.api.AllowedSortFields;
import com.lumie.common.api.PageResponse;
import com.lumie.tuition.application.dto.request.CancelInvoiceRequest;
import com.lumie.tuition.application.dto.request.IssueTuitionInvoiceRequest;
import com.lumie.tuition.application.dto.response.TuitionInvoiceResponse;
import com.lumie.tuition.application.service.TuitionInvoiceCommandService;
import com.lumie.tuition.application.service.TuitionInvoiceQueryService;
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
@RequestMapping("/v1/tuition-invoices")
@RequiredArgsConstructor
public class TuitionInvoiceController {

    private final TuitionInvoiceCommandService invoiceCommandService;
    private final TuitionInvoiceQueryService invoiceQueryService;

    @PostMapping
    public ResponseEntity<TuitionInvoiceResponse> issueInvoice(
            @Valid @RequestBody IssueTuitionInvoiceRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        IssueTuitionInvoiceRequest withKey = idempotencyKey != null
                ? request.withIdempotencyKey(idempotencyKey)
                : request;
        TuitionInvoiceResponse response = invoiceCommandService.issueInvoice(withKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TuitionInvoiceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceQueryService.getById(id));
    }

    @GetMapping
    @AllowedSortFields({"createdAt", "dueDate", "totalAmount", "status"})
    public ResponseEntity<PageResponse<TuitionInvoiceResponse>> listInvoices(
            @RequestParam(required = false) Long studentId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        var page = studentId != null
                ? invoiceQueryService.listByStudent(studentId, pageable)
                : invoiceQueryService.listAll(pageable);
        return ResponseEntity.ok(PageResponse.from(page));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<TuitionInvoiceResponse> cancelInvoice(
            @PathVariable Long id,
            @Valid @RequestBody CancelInvoiceRequest request) {
        return ResponseEntity.ok(invoiceCommandService.cancelInvoice(id, request));
    }
}
