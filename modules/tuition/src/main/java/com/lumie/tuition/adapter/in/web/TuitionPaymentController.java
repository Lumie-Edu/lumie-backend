package com.lumie.tuition.adapter.in.web;

import com.lumie.common.api.AllowedSortFields;
import com.lumie.common.api.PageResponse;
import com.lumie.tuition.application.dto.request.RefundRequest;
import com.lumie.tuition.application.dto.request.RequestPaymentRequest;
import com.lumie.tuition.application.dto.response.TuitionPaymentResponse;
import com.lumie.tuition.application.service.TuitionPaymentCommandService;
import com.lumie.tuition.application.service.TuitionPaymentQueryService;
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
@RequestMapping("/v1/tuition-payments")
@RequiredArgsConstructor
public class TuitionPaymentController {

    private final TuitionPaymentCommandService paymentCommandService;
    private final TuitionPaymentQueryService paymentQueryService;

    @PostMapping
    public ResponseEntity<TuitionPaymentResponse> requestPayment(
            @Valid @RequestBody RequestPaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        TuitionPaymentResponse response = paymentCommandService.requestPayment(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TuitionPaymentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentQueryService.getById(id));
    }

    @GetMapping
    @AllowedSortFields({"createdAt", "amount", "status"})
    public ResponseEntity<PageResponse<TuitionPaymentResponse>> listPayments(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(paymentQueryService.listAll(pageable)));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<TuitionPaymentResponse> refundPayment(
            @PathVariable Long id,
            @Valid @RequestBody RefundRequest request) {
        return ResponseEntity.ok(paymentCommandService.refundPayment(id, request));
    }
}
