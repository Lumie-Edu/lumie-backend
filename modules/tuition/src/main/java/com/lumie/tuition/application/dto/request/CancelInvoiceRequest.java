package com.lumie.tuition.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CancelInvoiceRequest(
        @NotBlank String reason
) {}
