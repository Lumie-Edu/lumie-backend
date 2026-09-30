package com.lumie.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Same public error envelope for failures handled before MVC exception advice. */
@Component
@RequiredArgsConstructor
public class ProblemResponseWriter {
    private final ObjectMapper objectMapper;

    public void write(HttpServletRequest request, HttpServletResponse response, int status,
                      String code, String detail) throws IOException {
        HttpStatus known = HttpStatus.resolve(status);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "urn:lumie:error:" + code.toLowerCase(java.util.Locale.ROOT).replace('_', '-'));
        body.put("title", known == null ? "Request failed" : known.getReasonPhrase());
        body.put("status", status);
        body.put("detail", detail);
        body.put("instance", request.getRequestURI());
        body.put("code", code);
        body.put("timestamp", Instant.now().toString());
        String tenant = MDC.get("tenantSlug");
        if (tenant != null) body.put("tenantSlug", tenant);
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
