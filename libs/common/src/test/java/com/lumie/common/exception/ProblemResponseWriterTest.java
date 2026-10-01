package com.lumie.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.assertThat;

class ProblemResponseWriterTest {
    @Test void filterFailureHasSameTopLevelProblemFieldsAsMvcFailure() throws Exception {
        var writer = new ProblemResponseWriter(new ObjectMapper());
        var request = new MockHttpServletRequest("POST", "/v1/example");
        var response = new MockHttpServletResponse();
        writer.write(request, response, 401, CommonErrorCode.UNAUTHORIZED.getCode(), "Unauthorized");
        var json = new ObjectMapper().readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(json.path("status").asInt()).isEqualTo(401);
        assertThat(json.path("instance").asText()).isEqualTo("/v1/example");
        assertThat(json.path("code").asText()).isEqualTo(CommonErrorCode.UNAUTHORIZED.getCode());
        assertThat(json.path("timestamp").asText()).isNotBlank();
        assertThat(json.path("properties").isMissingNode()).isTrue();
    }

    @Test void filterFailureWritesNonAsciiDetailAsUtf8() throws Exception {
        var writer = new ProblemResponseWriter(new ObjectMapper());
        var response = new MockHttpServletResponse();
        String detail = "요청 학원과 인증 학원이 일치하지 않습니다.";

        writer.write(new MockHttpServletRequest("PATCH", "/v1/tenants/other"), response,
                403, CommonErrorCode.FORBIDDEN.getCode(), detail);

        assertThat(response.getCharacterEncoding()).isEqualTo(StandardCharsets.UTF_8.name());
        assertThat(response.getContentType()).contains("application/problem+json");
        assertThat(new String(response.getContentAsByteArray(), StandardCharsets.UTF_8)).contains(detail);
    }
}
