package com.lumie.app.config;

import com.lumie.app.config.internal.InternalHmacAuthFilter;
import com.lumie.common.exception.ProblemResponseWriter;
import com.lumie.common.exception.CommonErrorCode;
import com.lumie.auth.adapter.in.security.SessionAuthenticationFilter;
import com.lumie.auth.adapter.in.security.SameOriginCsrfFilter;
import com.lumie.tenant.adapter.in.web.HostTenantFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import jakarta.servlet.Filter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SessionAuthenticationFilter sessionAuthenticationFilter;
    private final SameOriginCsrfFilter sameOriginCsrfFilter;
    private final HostTenantFilter hostTenantFilter;
    private final InternalHmacAuthFilter internalHmacAuthFilter;
    private final CorsConfigurationSource corsConfigurationSource;
    private final ProblemResponseWriter problems;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ASYNC, jakarta.servlet.DispatcherType.FORWARD, jakarta.servlet.DispatcherType.ERROR).permitAll()
                        .requestMatchers("/v1/register/owner").permitAll()
                        .requestMatchers("/v1/login").permitAll()
                        .requestMatchers("/tenant-root").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/internal/**").hasRole("INTERNAL")
                        .requestMatchers("/v1/payments/paddle/webhook").permitAll()
                        .requestMatchers("/v1/sms/solapi/webhook").permitAll()
                        .requestMatchers("/v1/tenants/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/tenants/*/logo").permitAll()
                        .requestMatchers("/v1/sms/**", "/v1/sms-templates/**")
                        .hasAnyRole("OWNER", "MANAGER", "INSTRUCTOR")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, exception) ->
                                problems.write(request, response, 401, CommonErrorCode.UNAUTHORIZED.getCode(), "Unauthorized"))
                        .accessDeniedHandler((request, response, exception) ->
                                problems.write(request, response, 403, CommonErrorCode.FORBIDDEN.getCode(), "Forbidden"))
                )
                .addFilterBefore(hostTenantFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(sessionAuthenticationFilter, HostTenantFilter.class)
                .addFilterBefore(sameOriginCsrfFilter, SessionAuthenticationFilter.class)
                .addFilterBefore(internalHmacAuthFilter, SessionAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public FilterRegistrationBean<HostTenantFilter> disableHostTenantServletRegistration(HostTenantFilter filter) {
        return disableServletRegistration(filter);
    }

    @Bean
    public FilterRegistrationBean<SessionAuthenticationFilter> disableSessionAuthenticationServletRegistration(
            SessionAuthenticationFilter filter) {
        return disableServletRegistration(filter);
    }

    @Bean
    public FilterRegistrationBean<SameOriginCsrfFilter> disableSameOriginCsrfServletRegistration(
            SameOriginCsrfFilter filter) {
        return disableServletRegistration(filter);
    }

    @Bean
    public FilterRegistrationBean<InternalHmacAuthFilter> disableInternalHmacServletRegistration(
            InternalHmacAuthFilter filter) {
        return disableServletRegistration(filter);
    }

    private static <T extends Filter> FilterRegistrationBean<T> disableServletRegistration(T filter) {
        FilterRegistrationBean<T> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
