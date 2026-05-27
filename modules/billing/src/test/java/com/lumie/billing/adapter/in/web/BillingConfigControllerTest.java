package com.lumie.billing.adapter.in.web;

import com.lumie.billing.application.dto.response.BillingConfigResponse;
import com.lumie.billing.application.service.BillingConfigQueryService;
import com.lumie.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillingConfigController")
class BillingConfigControllerTest {

    @Mock private BillingConfigQueryService billingConfigQueryService;
    @InjectMocks private BillingConfigController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /v1/billing/config — 200 with client key + sandbox flag, no secret leakage")
    void getBillingConfig_success() throws Exception {
        when(billingConfigQueryService.getPublicConfig())
                .thenReturn(new BillingConfigResponse("test_ck_publishable", true));

        mockMvc.perform(get("/v1/billing/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tossClientKey").value("test_ck_publishable"))
                .andExpect(jsonPath("$.sandbox").value(true))
                // Regression guard: BillingProperties.Toss has secretKey + webhookSecret
                // siblings. The response DTO must never include them — if a future change
                // widens BillingConfigResponse without thought this assertion will fail.
                .andExpect(content().string(not(containsString("secretKey"))))
                .andExpect(content().string(not(containsString("webhookSecret"))));
    }

    @Test
    @DisplayName("GET /v1/billing/config — Cache-Control: private, max-age=3600")
    void getBillingConfig_cacheable() throws Exception {
        when(billingConfigQueryService.getPublicConfig())
                .thenReturn(new BillingConfigResponse("test_ck_publishable", true));

        mockMvc.perform(get("/v1/billing/config"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=3600")))
                .andExpect(header().string("Cache-Control", containsString("private")));
    }
}
