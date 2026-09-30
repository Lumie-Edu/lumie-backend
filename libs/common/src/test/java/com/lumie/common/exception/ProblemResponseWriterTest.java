package com.lumie.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
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
}
