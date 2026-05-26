package com.lumie.ai.adapter.out.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@Configuration
public class AiChatConfig {

    /**
     * Spring-managed executor used by {@link com.lumie.ai.adapter.out.external.ChatbotClient}
     * for the BE→worker SSE proxy. Virtual threads keep the executor cheap to
     * scale per concurrent stream and carry a recognizable name in thread
     * dumps / metrics. NOTE: {@code SimpleAsyncTaskExecutor} does NOT
     * participate in Spring's graceful-shutdown drain — a container shutdown
     * mid-SSE will cut the stream; the FE is expected to reconnect.
     */
    @Bean
    public AsyncTaskExecutor aiChatExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("ai-chat-");
        executor.setVirtualThreads(true);
        return executor;
    }
}
