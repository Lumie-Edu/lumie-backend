package com.lumie.tuition.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SubmitKycRequest(
        @NotBlank String businessRegNumber,
        @NotBlank String representativeName,
        @NotBlank String settlementBankCode,
        @NotBlank String settlementAccountNumber,
        @NotBlank String settlementAccountHolder,
        String businessRegistrationDocUrl,
        String bankbookDocUrl
) {}
