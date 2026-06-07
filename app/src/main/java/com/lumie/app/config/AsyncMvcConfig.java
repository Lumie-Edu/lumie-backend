package com.lumie.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Ensures that Spring MVC's async processing (like StreamingResponseBody) uses the
 * auto-configured applicationTaskExecutor which is decorated by TenantAwareTaskDecorator.
 */
@Configuration
public class AsyncMvcConfig implements WebMvcConfigurer {

    private final AsyncTaskExecutor applicationTaskExecutor;

    public AsyncMvcConfig(AsyncTaskExecutor applicationTaskExecutor) {
        this.applicationTaskExecutor = applicationTaskExecutor;
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(applicationTaskExecutor);
    }
}
