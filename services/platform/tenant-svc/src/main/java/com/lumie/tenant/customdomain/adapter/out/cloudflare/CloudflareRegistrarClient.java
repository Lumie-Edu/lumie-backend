package com.lumie.tenant.customdomain.adapter.out.cloudflare;

import com.lumie.tenant.customdomain.application.port.out.DomainAvailabilityPort;
import com.lumie.tenant.customdomain.application.port.out.DomainAvailabilityResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(name = "cloudflare.registrar.enabled", havingValue = "true")
public class CloudflareRegistrarClient implements DomainAvailabilityPort {

    private static final String BASE_URL = "https://api.cloudflare.com/client/v4";

    private final RestClient restClient;
    private final String accountId;

    public CloudflareRegistrarClient(
            @Value("${cloudflare.registrar.account-id}") String accountId,
            @Value("${cloudflare.registrar.api-token}") String apiToken) {
        this.accountId = accountId;
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public DomainAvailabilityResult check(String domain) {
        String path = "/accounts/" + accountId + "/registrar/domain-check";
        try {
            CheckResponse resp = restClient.post()
                    .uri(path)
                    .body(Map.of("domains", List.of(domain)))
                    .retrieve()
                    .body(CheckResponse.class);

            if (resp == null || resp.result == null || resp.result.domains == null
                    || resp.result.domains.isEmpty()) {
                log.warn("Empty CF registrar response for {}", domain);
                return DomainAvailabilityResult.unavailable("empty_response");
            }
            DomainEntry entry = resp.result.domains.get(0);
            if (Boolean.TRUE.equals(entry.registrable)) {
                String price = entry.pricing != null ? entry.pricing.registrationCost : null;
                String currency = entry.pricing != null ? entry.pricing.currency : null;
                return DomainAvailabilityResult.available(price, currency);
            }
            return DomainAvailabilityResult.unavailable(entry.reason);
        } catch (RestClientResponseException e) {
            log.error("CF registrar check failed for {}: {} {}", domain, e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("Cloudflare registrar API error", e);
        }
    }

    record CheckResponse(Result result, boolean success) {}
    record Result(List<DomainEntry> domains) {}
    record DomainEntry(String name, Boolean registrable, String tier, String reason, Pricing pricing) {}
    record Pricing(String currency,
                   @com.fasterxml.jackson.annotation.JsonProperty("registration_cost") String registrationCost,
                   @com.fasterxml.jackson.annotation.JsonProperty("renewal_cost") String renewalCost) {}
}
