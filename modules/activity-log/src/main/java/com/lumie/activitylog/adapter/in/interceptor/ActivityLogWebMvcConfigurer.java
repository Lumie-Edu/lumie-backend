package com.lumie.activitylog.adapter.in.interceptor;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class ActivityLogWebMvcConfigurer implements WebMvcConfigurer {

    private final ActivityLogInterceptor activityLogInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(activityLogInterceptor)
                .addPathPatterns("/v1/**")
                .excludePathPatterns(
                        "/v1/login",
                        "/v1/register/**",
                        "/v1/refresh",
                        "/v1/logout",
                        "/v1/logout-all",
                        "/v1/oauth2/**",
                        "/v1/activity-logs/**",
                        "/v1/plans/**",
                        "/v1/payments/**",
                        "/v1/subscriptions/**",
                        "/v1/tenants/**",
                        "/v1/permissions/**"
                );
    }
}
