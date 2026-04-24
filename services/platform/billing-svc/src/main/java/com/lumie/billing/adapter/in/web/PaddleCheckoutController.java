package com.lumie.billing.adapter.in.web;

import com.lumie.billing.application.port.in.GetCheckoutSettingsUseCase;
import com.lumie.billing.application.port.in.GetCheckoutSettingsUseCase.CheckoutSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/billing")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "paddle.enabled", havingValue = "true")
public class PaddleCheckoutController {

    private final GetCheckoutSettingsUseCase getCheckoutSettingsUseCase;

    @GetMapping("/checkout-settings")
    public ResponseEntity<CheckoutSettings> getCheckoutSettings() {
        return ResponseEntity.ok(getCheckoutSettingsUseCase.getCheckoutSettings());
    }
}
