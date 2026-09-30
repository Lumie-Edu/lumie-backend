package com.lumie.exam.adapter.out.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.common.exception.BusinessException;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OmrServiceClientTest {

    private HttpServer server;
    private OmrServiceClient client;
    private final AtomicReference<String> path = new AtomicReference<>();
    private final AtomicReference<String> tenant = new AtomicReference<>();
    private final AtomicReference<String> requestBody = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            path.set(exchange.getRequestURI().getPath());
            tenant.set(exchange.getRequestHeaders().getFirst("X-Tenant-Slug"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"totalScore\":4,\"grade\":2,\"phoneNumber\":\"12345678\",\"results\":[]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var body = exchange.getResponseBody()) {
                body.write(response);
            }
        });
        server.start();
        client = new OmrServiceClient(RestClient.builder(), new ObjectMapper());
        ReflectionTestUtils.setField(client, "gradingServiceUrl",
                "http://" + server.getAddress().getAddress().getHostAddress() + ":" + server.getAddress().getPort());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void sendsMultipartToDeployedWorkerRouteWithTenant() {
        var result = client.gradeOmrImage(new byte[]{1, 2, 3}, "academy",
                Map.of("1", "a\"b"), Map.of("1", 4), Map.of("1", "SHORT"));

        assertThat(path.get()).isEqualTo("/api/temp/grading");
        assertThat(tenant.get()).isEqualTo("academy");
        assertThat(requestBody.get()).contains("{\"1\":\"a\\\"b\"}");
        assertThat(result.totalScore()).isEqualTo(4);
    }

    @Test
    void keepsSerializationFailureAsCause() {
        ObjectMapFailure failure = new ObjectMapFailure();
        OmrServiceClient broken = new OmrServiceClient(RestClient.builder(), failure);
        ReflectionTestUtils.setField(broken, "gradingServiceUrl",
                "http://" + server.getAddress().getAddress().getHostAddress() + ":" + server.getAddress().getPort());

        assertThatThrownBy(() -> broken.gradeOmrImage(new byte[]{1}, "academy",
                Map.of("1", "2"), Map.of(), Map.of()))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ExamErrorCode.OMR_GRADING_FAILED);
                    assertThat(exception.getCause()).isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class);
                });
    }

    private static class ObjectMapFailure extends ObjectMapper {
        @Override
        public String writeValueAsString(Object value) throws com.fasterxml.jackson.core.JsonProcessingException {
            throw new com.fasterxml.jackson.core.JsonProcessingException("serialization failed") {};
        }
    }
}
