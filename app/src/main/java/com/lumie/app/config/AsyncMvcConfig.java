package com.lumie.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Ensures that Spring MVC's async processing (like StreamingResponseBody) uses a
 * ThreadPoolTaskExecutor decorated by TenantAwareTaskDecorator.
 */
@Configuration
public class AsyncMvcConfig implements WebMvcConfigurer {

    private final TaskDecorator tenantAwareTaskDecorator;

    public AsyncMvcConfig(TaskDecorator tenantAwareTaskDecorator) {
        this.tenantAwareTaskDecorator = tenantAwareTaskDecorator;
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("mvc-async-");
        executor.setTaskDecorator(tenantAwareTaskDecorator);
        executor.initialize();
        configurer.setTaskExecutor(executor);
    }
}
