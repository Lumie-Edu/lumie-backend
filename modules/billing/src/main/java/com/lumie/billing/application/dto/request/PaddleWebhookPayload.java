package com.lumie.billing.application.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaddleWebhookPayload(
        @JsonProperty("event_id") String eventId,
        @JsonProperty("event_type") String eventType,
        @JsonProperty("occurred_at") String occurredAt,
        @JsonProperty("notification_id") String notificationId,
        Map<String, Object> data
) {

    public String dataString(String key) {
        return data != null ? (String) data.get(key) : null;
    }

    public String customDataString(String key) {
        if (data == null) return null;
        @SuppressWarnings("unchecked")
        var customData = (Map<String, Object>) data.get("custom_data");
        return customData != null ? (String) customData.get(key) : null;
    }

    public String firstPriceId() {
        if (data == null) return null;
        @SuppressWarnings("unchecked")
        var items = (List<Map<String, Object>>) data.get("items");
        if (items == null || items.isEmpty()) return null;
        @SuppressWarnings("unchecked")
        var price = (Map<String, Object>) items.get(0).get("price");
        return price != null ? (String) price.get("id") : null;
    }

    public String transactionAmount() {
        if (data == null) return null;
        @SuppressWarnings("unchecked")
        var details = (Map<String, Object>) data.get("details");
        if (details == null) return null;
        @SuppressWarnings("unchecked")
        var totals = (Map<String, Object>) details.get("totals");
        return totals != null ? String.valueOf(totals.get("total")) : null;
    }

    public String transactionCurrency() {
        if (data == null) return null;
        return (String) data.get("currency_code");
    }
}
