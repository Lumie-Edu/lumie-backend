package com.lumie.billing.adapter.out.external;

import com.lumie.billing.application.port.out.PaddleGatewayPort;
import com.lumie.billing.domain.exception.PaymentFailedException;
import com.lumie.billing.adapter.out.config.PaddleConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(name = "paddle.enabled", havingValue = "true")
public class PaddleClient implements PaddleGatewayPort {

    private final RestClient restClient;

    public PaddleClient(PaddleConfig config) {
        this.restClient = RestClient.builder()
                .baseUrl(config.getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public PaddleSubscription getSubscription(String paddleSubscriptionId) {
        log.info("Fetching Paddle subscription: {}", paddleSubscriptionId);
        Map<String, Object> response = restClient.get()
                .uri("/subscriptions/{id}", paddleSubscriptionId)
                .retrieve()
                .body(Map.class);
        return mapSubscription((Map<String, Object>) response.get("data"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public PaddleSubscription cancelSubscription(String paddleSubscriptionId, boolean effectAtPeriodEnd) {
        log.info("Cancelling Paddle subscription: {}, effectAtPeriodEnd={}", paddleSubscriptionId, effectAtPeriodEnd);
        Map<String, Object> body = Map.of(
                "effective_from", effectAtPeriodEnd ? "next_billing_period" : "immediately"
        );
        Map<String, Object> response = restClient.post()
                .uri("/subscriptions/{id}/cancel", paddleSubscriptionId)
                .body(body)
                .retrieve()
                .body(Map.class);
        return mapSubscription((Map<String, Object>) response.get("data"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public PaddleSubscription updateSubscription(String paddleSubscriptionId, String newPriceId) {
        log.info("Updating Paddle subscription: {}, newPriceId={}", paddleSubscriptionId, newPriceId);
        Map<String, Object> body = Map.of(
                "items", java.util.List.of(Map.of("price_id", newPriceId, "quantity", 1)),
                "proration_billing_mode", "prorated_immediately"
        );
        Map<String, Object> response = restClient.patch()
                .uri("/subscriptions/{id}", paddleSubscriptionId)
                .body(body)
                .retrieve()
                .body(Map.class);
        return mapSubscription((Map<String, Object>) response.get("data"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public PaddleCustomer createCustomer(String email, String name) {
        log.info("Creating Paddle customer: email={}", email);
        Map<String, Object> body = Map.of("email", email, "name", name);
        Map<String, Object> response = restClient.post()
                .uri("/customers")
                .body(body)
                .retrieve()
                .body(Map.class);
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        return new PaddleCustomer(
                (String) data.get("id"),
                (String) data.get("email"),
                (String) data.get("name")
        );
    }

    @SuppressWarnings("unchecked")
    private PaddleSubscription mapSubscription(Map<String, Object> data) {
        if (data == null) {
            throw new PaymentFailedException("Empty Paddle subscription response");
        }
        String priceId = null;
        var items = (java.util.List<Map<String, Object>>) data.get("items");
        if (items != null && !items.isEmpty()) {
            var price = (Map<String, Object>) items.get(0).get("price");
            if (price != null) {
                priceId = (String) price.get("id");
            }
        }
        return new PaddleSubscription(
                (String) data.get("id"),
                (String) data.get("customer_id"),
                (String) data.get("status"),
                priceId,
                (String) data.get("next_billed_at")
        );
    }
}
