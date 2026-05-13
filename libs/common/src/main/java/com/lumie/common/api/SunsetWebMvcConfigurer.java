package com.lumie.common.api;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers {@link SunsetInterceptor} for all routes.
 * The interceptor is a no-op for handlers without {@link Sunset}, so blanket registration
 * keeps the per-endpoint opt-in clean.
 */
@Configuration
@RequiredArgsConstructor
public class SunsetWebMvcConfigurer implements WebMvcConfigurer {

    private final SunsetInterceptor sunsetInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sunsetInterceptor);
    }
}
