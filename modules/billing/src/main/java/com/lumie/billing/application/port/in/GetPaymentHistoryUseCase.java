package com.lumie.billing.application.port.in;

import com.lumie.billing.application.dto.response.InvoiceResponse;

import java.util.List;

public interface GetPaymentHistoryUseCase {
    List<InvoiceResponse> getPaymentHistory(String tenantSlug);
}
