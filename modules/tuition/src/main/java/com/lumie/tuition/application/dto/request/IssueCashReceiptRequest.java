package com.lumie.tuition.application.dto.request;

import com.lumie.tuition.domain.vo.CashReceiptType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IssueCashReceiptRequest(
        @NotNull Long tuitionPaymentId,
        @NotNull CashReceiptType receiptType,
        @NotBlank String identifier,
        String customerName
) {}
