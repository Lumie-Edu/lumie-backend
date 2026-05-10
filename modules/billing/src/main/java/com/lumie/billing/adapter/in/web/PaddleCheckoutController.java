package com.lumie.billing.adapter.in.web;

import com.lumie.billing.application.service.CheckoutSettingsService;
import com.lumie.billing.application.service.CheckoutSettingsService.CheckoutSettings;
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

    private final CheckoutSettingsService checkoutSettingsService;

    @GetMapping("/checkout-settings")
    public ResponseEntity<CheckoutSettings> getCheckoutSettings() {
        return ResponseEntity.ok(checkoutSettingsService.getCheckoutSettings());
    }
}
