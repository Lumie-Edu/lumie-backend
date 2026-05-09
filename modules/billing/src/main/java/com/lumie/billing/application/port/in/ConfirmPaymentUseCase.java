package com.lumie.billing.application.port.in;

import com.lumie.billing.application.dto.request.ConfirmPaymentRequest;
import com.lumie.billing.application.dto.response.PaymentResponse;

public interface ConfirmPaymentUseCase {
    PaymentResponse confirmPayment(ConfirmPaymentRequest request);
}
