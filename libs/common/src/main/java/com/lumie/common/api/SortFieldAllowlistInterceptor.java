package com.lumie.common.api;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * Pre-handler guard that enforces {@link AllowedSortFields} on annotated controller methods.
 *
 * <p>Reads each {@code ?sort=field[,direction]} query parameter, extracts the property
 * name (text before the first comma), and rejects the request with a
 * {@link BusinessException}{@code (CommonErrorCode.INVALID_INPUT)} (mapped to
 * {@code 400 Bad Request} by {@code GlobalExceptionHandler}) if any property is outside
 * the annotation's allowlist.
 *
 * <p>Wired by {@link com.lumie.common.api.SortFieldAllowlistWebMvcConfigurer}. Bypasses
 * non-{@link HandlerMethod} handlers and methods without the annotation — zero overhead
 * on the vast majority of endpoints.
 */
@Component
public class SortFieldAllowlistInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        AllowedSortFields annotation = handlerMethod.getMethodAnnotation(AllowedSortFields.class);
        if (annotation == null) {
            return true;
        }
        String[] sortParams = request.getParameterValues("sort");
        if (sortParams == null || sortParams.length == 0) {
            return true;
        }
        Set<String> allowed = Set.of(annotation.value());
        for (String raw : sortParams) {
            if (raw == null || raw.isBlank()) continue;
            // Spring accepts `?sort=field,direction` and also `?sort=field1&sort=field2`.
            // Split on comma; first token is the property, optional trailing token is asc/desc.
            String property = raw.split(",", 2)[0].trim();
            if (property.isEmpty()) continue;
            if (!allowed.contains(property)) {
                throw new BusinessException(
                        CommonErrorCode.INVALID_INPUT,
                        "Sort property '" + property + "' is not allowed. Allowed: " + allowed);
            }
        }
        return true;
    }
}
