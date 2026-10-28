package com.lumie.exam.adapter.out.external;

import com.lumie.common.exception.BusinessException;
import com.lumie.exam.application.dto.request.ReportBatchRequest;
import com.lumie.exam.application.dto.request.UpdateAiCommentaryRequest;
import com.lumie.exam.application.dto.response.AiCommentaryResponse;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class JoossamengAiReportClientTest {

    private MockRestServiceServer server;
    private JoossamengAiReportClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new JoossamengAiReportClient(builder, "http://analysis-svc");
        ReflectionTestUtils.setField(client, "analysisServiceUrl", "http://analysis-svc");
    }

    @Test
    void getAiCommentary_forwardsTenantHeaderAndParsesResponse() {
        server.expect(requestTo("http://analysis-svc/api/joossameng/ai-commentary/exams/55"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Tenant-Slug", "inst-joossameng"))
                .andRespond(withSuccess("""
                        {
                          "content": "Generated commentary",
                          "isEdited": false,
                          "generatedAt": "2026-08-08T02:30:00Z",
                          "editedAt": null,
                          "version": 1
                        }
                        """, MediaType.APPLICATION_JSON));

        AiCommentaryResponse response = client.getAiCommentary(55L, "inst-joossameng");

        assertThat(response.content()).isEqualTo("Generated commentary");
        assertThat(response.isEdited()).isFalse();
        assertThat(response.version()).isEqualTo(1L);
        server.verify();
    }

    @Test
    void getAiCommentary_throwsBusinessExceptionOnWorker404() {
        server.expect(requestTo("http://analysis-svc/api/joossameng/ai-commentary/exams/55"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.getAiCommentary(55L, "inst-joossameng"))
                .isInstanceOf(com.lumie.common.exception.BusinessException.class)
                .hasMessageContaining("404");
        server.verify();
    }

    @Test
    void generateAiCommentary_postsToExpectedWorkerPath() {
        server.expect(requestTo("http://analysis-svc/api/joossameng/ai-commentary/exams/55/generate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Tenant-Slug", "inst-joossameng"))
                .andRespond(withSuccess("""
                        {
                          "content": "Generated commentary",
                          "isEdited": false,
                          "generatedAt": "2026-08-08T02:30:00Z",
                          "editedAt": null,
                          "version": 1
                        }
                        """, MediaType.APPLICATION_JSON));

        AiCommentaryResponse response = client.generateAiCommentary(55L, "inst-joossameng");

        assertThat(response.content()).isEqualTo("Generated commentary");
        server.verify();
    }

    @Test
    void updateAiCommentary_putsJsonBodyAndHeader() {
        server.expect(requestTo("http://analysis-svc/api/joossameng/ai-commentary/exams/55"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header("X-Tenant-Slug", "inst-joossameng"))
                .andExpect(content().json("""
                        {"content":"Edited commentary","expectedVersion":1}
                        """))
                .andRespond(withSuccess("""
                        {
                          "content": "Edited commentary",
                          "isEdited": true,
                          "generatedAt": "2026-08-08T02:30:00Z",
                          "editedAt": "2026-08-08T02:45:00Z",
                          "version": 2
                        }
                        """, MediaType.APPLICATION_JSON));

        AiCommentaryResponse response = client.updateAiCommentary(
                55L,
                new UpdateAiCommentaryRequest("Edited commentary", 1L),
                "inst-joossameng"
        );

        assertThat(response.isEdited()).isTrue();
        assertThat(response.version()).isEqualTo(2L);
        server.verify();
    }

    @Test
    void updateAiCommentary_mapsWorkerConflictToOptimisticLockConflict() {
        server.expect(requestTo("http://analysis-svc/api/joossameng/ai-commentary/exams/55"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> client.updateAiCommentary(
                55L,
                new UpdateAiCommentaryRequest("Edited commentary", 1L),
                "inst-joossameng"
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("다른 곳에서 항목이 수정되었습니다");
        server.verify();
    }

    @Test
    void generateBatch_postsStudentsAndReturnsZipBytes() throws IOException {
        String expectedPath = "/api/joossameng/ai-reports/exams/55/batch";
        AtomicReference<String> requestPath = new AtomicReference<>();
        AtomicReference<String> tenantHeader = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer worker = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        worker.createContext(expectedPath, exchange -> {
            requestPath.set(exchange.getRequestURI().getPath());
            tenantHeader.set(exchange.getRequestHeaders().getFirst("X-Tenant-Slug"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] zipBytes = {1, 2, 3};
            exchange.getResponseHeaders().set("Content-Type", "application/zip");
            exchange.sendResponseHeaders(HttpStatus.OK.value(), zipBytes.length);
            exchange.getResponseBody().write(zipBytes);
            exchange.close();
        });
        worker.start();

        try {
            String workerHost = worker.getAddress().getAddress().getHostAddress();
            if (workerHost.contains(":")) {
                workerHost = "[" + workerHost + "]";
            }
            client = new JoossamengAiReportClient(
                    RestClient.builder(),
                    "http://" + workerHost + ":" + worker.getAddress().getPort()
            );

            assertThat(client.generateBatch(
                    55L,
                    new ReportBatchRequest(java.util.List.of(7L, 8L)),
                    "inst-joossameng"
            )).containsExactly(1, 2, 3);
            assertThat(requestPath).hasValue(expectedPath);
            assertThat(tenantHeader).hasValue("inst-joossameng");
            assertThat(requestBody).hasValue("{\"studentIds\":[7,8]}");
        } finally {
            worker.stop(0);
        }
    }
}
