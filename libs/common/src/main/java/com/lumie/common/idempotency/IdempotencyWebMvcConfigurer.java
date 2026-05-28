package com.lumie.common.idempotency;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers {@link IdempotencyHeaderInterceptor} as a global interceptor.
 *
 * <p>The interceptor is a no-op for non-mutating methods, non-{@code /v1/**}
 * paths, and exempt patterns (webhook callbacks), so blanket registration
 * keeps the enforcement automatic for any future controller without
 * per-controller wiring.
 */
@Configuration
public class IdempotencyWebMvcConfigurer implements WebMvcConfigurer {

    @Bean
    public IdempotencyHeaderInterceptor idempotencyHeaderInterceptor() {
        return new IdempotencyHeaderInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(idempotencyHeaderInterceptor());
    }
}
