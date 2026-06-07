package com.lumie.common.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import static net.logstash.logback.argument.StructuredArguments.keyValue;

/**
 * Logs outbound HTTP calls with the same Loki-friendly shape as access logs.
 */
public class ExternalCallLoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger("external-call-log");

    @Override
    public ClientHttpResponse intercept(org.springframework.http.HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        long startNanos = System.nanoTime();
        try {
            ClientHttpResponse response = execution.execute(request, body);
            long latencyMs = elapsedMillis(startNanos);
            int status = response.getStatusCode().value();
            log.info("External call {} {} {} {}ms",
                    request.getMethod(), sanitizePath(request.getURI()), status, latencyMs,
                    keyValue("eventType", "external_call"),
                    keyValue("dependency", dependency(request.getURI())),
                    keyValue("operation", operation(request)),
                    keyValue("protocol", "http"),
                    keyValue("httpMethod", request.getMethod().name()),
                    keyValue("externalPath", sanitizePath(request.getURI())),
                    keyValue("externalStatus", status),
                    keyValue("latencyMs", latencyMs),
                    keyValue("outcome", outcome(status)));
            return response;
        } catch (IOException | RuntimeException e) {
            long latencyMs = elapsedMillis(startNanos);
            log.warn("External call {} {} failed {}ms",
                    request.getMethod(), sanitizePath(request.getURI()), latencyMs,
                    keyValue("eventType", "external_call"),
                    keyValue("dependency", dependency(request.getURI())),
                    keyValue("operation", operation(request)),
                    keyValue("protocol", "http"),
                    keyValue("httpMethod", request.getMethod().name()),
                    keyValue("externalPath", sanitizePath(request.getURI())),
                    keyValue("latencyMs", latencyMs),
                    keyValue("outcome", "failed"),
                    keyValue("exceptionClass", e.getClass().getSimpleName()));
            throw e;
        }
    }

    private long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    private String operation(org.springframework.http.HttpRequest request) {
        return request.getMethod().name() + " " + sanitizePath(request.getURI());
    }

    private String dependency(URI uri) {
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            return "relative";
        }
        return uri.getHost();
    }

    private String sanitizePath(URI uri) {
        String path = uri.getPath();
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path;
    }

    private String outcome(int status) {
        return status >= 500 ? "server_error"
                : status >= 400 ? "client_error"
                : "success";
    }
}
