package com.lumie.common.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Adds a {@code Sunset:} response header (RFC 8594) to endpoints annotated with {@link Sunset}.
 * The value is formatted as an IMF-fixdate (RFC 7231) as required by RFC 8594.
 * Method-level annotation takes precedence over type-level.
 */
@Component
public class SunsetInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }
        Sunset sunset = hm.getMethodAnnotation(Sunset.class);
        if (sunset == null) {
            sunset = hm.getBeanType().getAnnotation(Sunset.class);
        }
        if (sunset != null) {
            // Interpret annotation date as KST midnight, then convert to GMT for the IMF-fixdate wire format (RFC 8594).
            String httpDate = DateTimeFormatter.RFC_1123_DATE_TIME
                    .format(LocalDate.parse(sunset.value())
                            .atStartOfDay(ZoneId.of("Asia/Seoul"))
                            .withZoneSameInstant(ZoneOffset.UTC));
            response.setHeader("Sunset", httpDate);
            response.setHeader("Deprecation", "true");
        }
        return true;
    }
}
