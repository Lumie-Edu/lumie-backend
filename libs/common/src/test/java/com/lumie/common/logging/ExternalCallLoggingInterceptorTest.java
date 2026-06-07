package com.lumie.common.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExternalCallLoggingInterceptorTest {

    private final Logger externalLogger = (Logger) LoggerFactory.getLogger("external-call-log");
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @AfterEach
    void tearDown() {
        externalLogger.detachAppender(appender);
        appender.stop();
    }

    @Test
    @DisplayName("interceptor logs successful outbound HTTP calls as external_call")
    void intercept_success_logsExternalCallFields() throws IOException {
        ExternalCallLoggingInterceptor interceptor = new ExternalCallLoggingInterceptor();
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.POST,
                URI.create("https://api.example.com/v1/payments/confirm?secret=redacted"));
        appender.start();
        externalLogger.addAppender(appender);

        interceptor.intercept(request, new byte[0], (req, body) ->
                new MockClientHttpResponse("{}".getBytes(StandardCharsets.UTF_8), HttpStatus.CREATED));

        assertThat(appender.list).hasSize(1);
        ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getFormattedMessage()).startsWith("External call POST /v1/payments/confirm 201 ");
        assertThat(structuredArguments(event))
                .containsEntry("eventType", "external_call")
                .containsEntry("dependency", "api.example.com")
                .containsEntry("operation", "POST /v1/payments/confirm")
                .containsEntry("protocol", "http")
                .containsEntry("httpMethod", "POST")
                .containsEntry("externalPath", "/v1/payments/confirm")
                .containsEntry("externalStatus", "201")
                .containsEntry("outcome", "success");
    }

    @Test
    @DisplayName("interceptor logs failed outbound HTTP calls as external_call")
    void intercept_failure_logsExternalCallFields() {
        ExternalCallLoggingInterceptor interceptor = new ExternalCallLoggingInterceptor();
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET,
                URI.create("http://worker.local/internal/exams/1"));
        appender.start();
        externalLogger.addAppender(appender);

        assertThatThrownBy(() -> interceptor.intercept(request, new byte[0], (req, body) -> {
            throw new IOException("connection refused");
        })).isInstanceOf(IOException.class);

        assertThat(appender.list).hasSize(1);
        ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getFormattedMessage()).startsWith("External call GET /internal/exams/1 failed ");
        assertThat(structuredArguments(event))
                .containsEntry("eventType", "external_call")
                .containsEntry("dependency", "worker.local")
                .containsEntry("operation", "GET /internal/exams/1")
                .containsEntry("httpMethod", "GET")
                .containsEntry("externalPath", "/internal/exams/1")
                .containsEntry("outcome", "failed")
                .containsEntry("exceptionClass", "IOException");
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
