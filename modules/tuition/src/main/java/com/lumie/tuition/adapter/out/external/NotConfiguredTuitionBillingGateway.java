package com.lumie.tuition.adapter.out.external;

import com.lumie.tuition.application.port.out.TuitionBillingGatewayPort;
import com.lumie.tuition.domain.exception.TuitionErrorCode;
import com.lumie.tuition.domain.exception.TuitionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Placeholder implementation of {@link TuitionBillingGatewayPort} until the
 * PaySsam ("결제선생") adapter is wired.
 *
 * <p>Fails loudly rather than silently no-op'ing, so a misconfiguration surfaces
 * immediately. To go live, replace this bean with the real PaySsam adapter — this
 * is the single swap point for the integration.
 */
@Slf4j
@Component
public class NotConfiguredTuitionBillingGateway implements TuitionBillingGatewayPort {

    @Override
    public SendInvoiceResult sendInvoice(String orderId, String guardianPhone, String guardianName,
                                         String title, BigDecimal amount, Instant dueDate) {
        log.error("Tuition billing gateway not configured — sendInvoice rejected. orderId={}", orderId);
        throw new TuitionException(TuitionErrorCode.GATEWAY_NOT_CONFIGURED,
                "Tuition billing gateway is not configured. Wire the PaySsam adapter.");
    }

    @Override
    public CollectionStatusResult queryStatus(String externalInvoiceId) {
        log.error("Tuition billing gateway not configured — queryStatus rejected. externalInvoiceId={}",
                externalInvoiceId);
        throw new TuitionException(TuitionErrorCode.GATEWAY_NOT_CONFIGURED,
                "Tuition billing gateway is not configured. Wire the PaySsam adapter.");
    }
}
