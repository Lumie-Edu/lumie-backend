package com.lumie.common.api;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers {@link SortFieldAllowlistInterceptor} for all routes.
 *
 * <p>The interceptor is a no-op for handlers without {@link AllowedSortFields}, so a
 * blanket registration keeps the per-endpoint opt-in clean: just add the annotation,
 * and validation is in effect. No path patterns to maintain.
 */
@Configuration
@RequiredArgsConstructor
public class SortFieldAllowlistWebMvcConfigurer implements WebMvcConfigurer {

    private final SortFieldAllowlistInterceptor sortFieldAllowlistInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sortFieldAllowlistInterceptor);
    }
}
