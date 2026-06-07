package com.lumie.common.tenant;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AccessLogFilterTest {

    private final Logger accessLogger = (Logger) LoggerFactory.getLogger("access-log");
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @AfterEach
    void tearDown() {
        accessLogger.detachAppender(appender);
        appender.stop();
    }

    @Test
    @DisplayName("access log uses structured arguments without duplicating HTTP fields in MDC")
    void accessLog_emitsStructuredFieldsWithoutHttpMdcDuplicates() throws ServletException, IOException {
        AccessLogFilter filter = new AccessLogFilter(List.of());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/students/42");
        request.setRemoteAddr("10.0.0.9");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.9");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/v1/students/{studentId}");
        MockHttpServletResponse response = new MockHttpServletResponse();
        appender.start();
        accessLogger.addAppender(appender);

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                ((MockHttpServletResponse) servletResponse).setStatus(201));

        assertThat(appender.list).hasSize(1);
        ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getFormattedMessage())
                .startsWith("HTTP request POST /v1/students/{studentId} 201 ");
        assertThat(event.getMDCPropertyMap())
                .doesNotContainKeys("httpMethod", "httpPath", "httpStatus", "latencyMs");
        assertThat(structuredArguments(event))
                .containsEntry("eventType", "http_request")
                .containsEntry("httpMethod", "POST")
                .containsEntry("pathTemplate", "/v1/students/{studentId}")
                .containsEntry("rawPath", "/v1/students/42")
                .containsEntry("httpStatus", "201")
                .containsEntry("outcome", "success")
                .containsEntry("clientIp", "203.0.113.10");
    }

    private Map<String, String> structuredArguments(ILoggingEvent event) {
        return event.getArgumentArray() == null ? Map.of() :
                java.util.Arrays.stream(event.getArgumentArray())
                        .map(String::valueOf)
                        .filter(value -> value.contains("="))
                        .map(value -> value.split("=", 2))
                        .collect(java.util.stream.Collectors.toMap(parts -> parts[0], parts -> parts[1]));
    }
}
