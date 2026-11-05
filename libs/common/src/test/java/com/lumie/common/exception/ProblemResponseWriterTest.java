package com.lumie.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import static org.assertj.core.api.Assertions.assertThat;

class ProblemResponseWriterTest {
    @Test void filterFailureHasSameTopLevelProblemFieldsAsMvcFailure() throws Exception {
        var response = writeProblem(401, CommonErrorCode.UNAUTHORIZED.getCode(), "Unauthorized");
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
        String detail = "요청 학원과 인증 학원이 일치하지 않습니다.";
        var response = writeProblem(403, CommonErrorCode.FORBIDDEN.getCode(), detail);

        assertThat(response.getCharacterEncoding()).isEqualTo(StandardCharsets.UTF_8.name());
        assertThat(response.getContentType()).contains("application/problem+json");
        assertThat(new String(response.getContentAsByteArray(), StandardCharsets.UTF_8)).contains(detail);
    }

    @Test void filterFailureLocalizesEnglishDetailAndTitleButPreservesKoreanDetail() throws Exception {
        var english = new ObjectMapper().readTree(writeProblem(403, "COMMON_006", "Forbidden").getContentAsString());
        var korean = new ObjectMapper().readTree(writeProblem(429, "RATE_LIMITED", "요청이 너무 많습니다.").getContentAsString());

        assertThat(english.path("detail").asText()).isEqualTo("이 작업을 수행할 권한이 없습니다.");
        assertThat(english.path("title").asText()).isEqualTo("접근 거부");
        assertThat(korean.path("detail").asText()).isEqualTo("요청이 너무 많습니다.");
    }

    private static MockHttpServletResponse writeProblem(int status, String code, String detail)
            throws IOException {
        var writer = new ProblemResponseWriter(new ObjectMapper());
        var response = new MockHttpServletResponse();
        writer.write(new MockHttpServletRequest("POST", "/v1/example"), response, status, code, detail);
        return response;
    }
}
