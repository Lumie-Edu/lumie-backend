package com.lumie.tuition.application.dto.request;

import com.lumie.tuition.domain.vo.TuitionPaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RequestPaymentRequest(
        @NotNull Long invoiceId,
        @NotNull TuitionPaymentMethod method,
        @NotBlank String customerName,
        String customerEmail,
        @NotBlank String successUrl,
        @NotBlank String failUrl
) {}
