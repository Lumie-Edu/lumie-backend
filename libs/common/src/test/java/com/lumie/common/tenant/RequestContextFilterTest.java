package com.lumie.common.tenant;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RequestContextFilterTest {

    @AfterEach
    void clearContext() {
        MDC.clear();
        TenantContextHolder.clear();
        UserContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("generates requestId, writes it to response header, and clears MDC after request")
    void generatedRequestId_isAvailableDuringRequestAndClearedAfterwards() throws ServletException, IOException {
        RequestContextFilter filter = new RequestContextFilter(List.of());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/students");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] requestIdInsideChain = new String[1];

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            requestIdInsideChain[0] = MDC.get("requestId");
            assertThat(response.getHeader(RequestContextFilter.REQUEST_ID_HEADER))
                    .isEqualTo(requestIdInsideChain[0]);
        });

        assertThat(requestIdInsideChain[0]).isNotBlank();
        assertThat(response.getHeader(RequestContextFilter.REQUEST_ID_HEADER))
                .isEqualTo(requestIdInsideChain[0]);
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    @DisplayName("preserves inbound X-Request-Id for log correlation")
    void inboundRequestId_isPreserved() throws ServletException, IOException {
        RequestContextFilter filter = new RequestContextFilter(List.of());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/omr/grade");
        request.addHeader(RequestContextFilter.REQUEST_ID_HEADER, "req-owner-dashboard-1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] requestIdInsideChain = new String[1];

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                requestIdInsideChain[0] = MDC.get("requestId"));

        assertThat(requestIdInsideChain[0]).isEqualTo("req-owner-dashboard-1");
        assertThat(response.getHeader(RequestContextFilter.REQUEST_ID_HEADER))
                .isEqualTo("req-owner-dashboard-1");
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    @DisplayName("extracts valid W3C traceparent traceId into MDC")
    void traceparentTraceId_isAvailableDuringRequest() throws ServletException, IOException {
        RequestContextFilter filter = new RequestContextFilter(List.of());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/classes/7");
        request.addHeader("traceparent", "00-4BF92F3577B34DA6A3CE929D0E0E4736-00f067aa0ba902b7-01");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] traceIdInsideChain = new String[1];

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                traceIdInsideChain[0] = MDC.get("traceId"));

        assertThat(traceIdInsideChain[0]).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(MDC.get("traceId")).isNull();
    }
}
