package com.lumie.notification.application.service;

import com.lumie.common.exception.BusinessException;
import com.lumie.notification.adapter.out.config.SmsProperties;
import com.lumie.notification.application.port.out.SmsProviderPort;
import com.lumie.notification.domain.exception.SmsErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class SmsProviderRegistry {

    private final SmsProperties properties;
    private final List<SmsProviderPort> providers;

    @PostConstruct
    void validateSelectedProvider() {
        provider().validateConfiguration();
    }

    public SmsProviderPort provider() {
        String selected = properties.provider().toLowerCase(Locale.ROOT);
        return providers.stream()
                .filter(provider -> provider.providerName().equalsIgnoreCase(selected))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        SmsErrorCode.PROVIDER_CONFIGURATION_INVALID,
                        "Unsupported SMS provider: " + properties.provider()
                ));
    }

    public boolean isMockProvider() {
        return "mock".equalsIgnoreCase(properties.provider());
    }
}
