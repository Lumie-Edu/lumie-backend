package com.lumie.ai.adapter.out.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ChatbotClientTest {

    @Test
    void proxiesWorkerErrorFrameWithoutChangingEventOrData() throws IOException {
        var client = new ChatbotClient(RestClient.builder(), mock(AsyncTaskExecutor.class),
                new ObjectMapper(), "http://chatbot-svc");
        var emitter = new CapturingSseEmitter();
        String frame = "event: error\ndata: {\"message\":\"처리 중 오류가 발생했습니다.\"}\n\n";

        client.proxyFrames(new BufferedReader(new StringReader(frame)), emitter, new AtomicBoolean(false));

        String serializedParts = String.join("", emitter.parts);
        assertThat(serializedParts).contains("event:error", "data:{\"message\":\"처리 중 오류가 발생했습니다.\"}");
    }

    private static final class CapturingSseEmitter extends SseEmitter {
        private final List<String> parts = new ArrayList<>();

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            builder.build().forEach(part -> parts.add(String.valueOf(part.getData())));
        }
    }
}
