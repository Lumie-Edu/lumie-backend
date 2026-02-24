package com.lumie.ai.adapter.out.external;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Component
public class InternalServiceClient {

    private final WebClient webClient;

    public InternalServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> post(String baseUrl, String path, Map<String, Object> body) {
        log.info("Internal service call: POST {}{}",  baseUrl, path);

        return webClient.post()
                .uri(baseUrl + path)
                .header("X-User-Id", String.valueOf(UserContextHolder.getUserId()))
                .header("X-Tenant-Slug", TenantContextHolder.getTenantSlug())
                .header("X-Tenant-Id", String.valueOf(TenantContextHolder.getTenantId()))
                .header("X-User-Role", UserContextHolder.getUserRole())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }
}
