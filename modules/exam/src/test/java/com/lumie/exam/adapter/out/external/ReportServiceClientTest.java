package com.lumie.exam.adapter.out.external;

import com.lumie.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ReportServiceClientTest {

    private MockRestServiceServer server;
    private ReportServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ReportServiceClient(builder);
        ReflectionTestUtils.setField(client, "reportServiceUrl", "http://report-svc");
    }

    @Test
    void generateReport_returnsPdfBytes() {
        byte[] pdfBytes = "%PDF-1.7\nbody".getBytes(StandardCharsets.US_ASCII);
        server.expect(requestTo("http://report-svc/v1/reports/students/99/exams/10"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(pdfBytes, MediaType.APPLICATION_PDF));

        byte[] result = client.generateReport(99L, 10L, "test-tenant");

        assertThat(result).isEqualTo(pdfBytes);
        server.verify();
    }

    @Test
    void generateReport_rejectsNonPdfSuccessBody() {
        server.expect(requestTo("http://report-svc/v1/reports/students/99/exams/10"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(new byte[]{(byte) 0xFF, (byte) 0xD8}, MediaType.IMAGE_JPEG));

        assertThatThrownBy(() -> client.generateReport(99L, 10L, "test-tenant"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("non-PDF");
        server.verify();
    }

    @Test
    void generateReport_rejectsEmptySuccessBody() {
        server.expect(requestTo("http://report-svc/v1/reports/students/99/exams/10"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(new byte[0], MediaType.APPLICATION_PDF));

        assertThatThrownBy(() -> client.generateReport(99L, 10L, "test-tenant"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("non-PDF");
        server.verify();
    }
}
