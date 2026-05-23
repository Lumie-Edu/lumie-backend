package com.lumie.activitylog.application.service;

import com.lumie.activitylog.application.dto.request.WebVitalsReportRequest;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Records frontend Core Web Vitals into Micrometer as a {@link DistributionSummary}, exposed via
 * the module's existing {@code /actuator/prometheus} scrape. No database access, no transaction.
 *
 * <p>The reporting endpoint is public (permitAll), so every tag is bounded to a small, known set to
 * prevent Prometheus cardinality blow-up from hostile or buggy clients: {@code metric} and
 * {@code rating} are allow-listed; {@code route} is character/length-clamped and falls back to
 * {@code "unknown"}. Summaries are cached per tag-combination so the hot path skips the builder +
 * registry lookup on every beacon.
 */
@Service
@RequiredArgsConstructor
public class WebVitalsRecordingService {

    private static final String METRIC_NAME = "frontend.web.vitals";
    private static final Set<String> KNOWN_METRICS = Set.of("lcp", "cls", "inp", "fcp", "ttfb");
    private static final Set<String> KNOWN_RATINGS = Set.of("good", "needs-improvement", "poor");
    private static final int MAX_ROUTE_LENGTH = 80;
    private static final Pattern SAFE_ROUTE = Pattern.compile("^/[A-Za-z0-9/_\\[\\]-]*$");

    private final MeterRegistry meterRegistry;
    private final Map<String, DistributionSummary> summaries = new ConcurrentHashMap<>();

    public void record(WebVitalsReportRequest request) {
        String metric = normalizeMetric(request.metric());
        String rating = normalizeRating(request.rating());
        String route = normalizeRoute(request.route());
        summaries.computeIfAbsent(metric + '|' + rating + '|' + route, key ->
                        DistributionSummary.builder(METRIC_NAME)
                                .description("Core Web Vitals reported by the frontend")
                                .tag("metric", metric)
                                .tag("rating", rating)
                                .tag("route", route)
                                .publishPercentiles(0.5, 0.75, 0.95)
                                .register(meterRegistry))
                .record(request.value());
    }

    private String normalizeMetric(String metric) {
        if (metric == null) {
            return "other";
        }
        String lower = metric.toLowerCase(Locale.ROOT);
        return KNOWN_METRICS.contains(lower) ? lower : "other";
    }

    private String normalizeRating(String rating) {
        return rating != null && KNOWN_RATINGS.contains(rating) ? rating : "unknown";
    }

    private String normalizeRoute(String route) {
        if (route == null || route.isBlank()) {
            return "unknown";
        }
        String path = route.split("\\?", 2)[0];
        if (path.length() > MAX_ROUTE_LENGTH) {
            int cut = path.lastIndexOf('/', MAX_ROUTE_LENGTH);
            path = cut > 0 ? path.substring(0, cut) : path.substring(0, MAX_ROUTE_LENGTH);
        }
        return SAFE_ROUTE.matcher(path).matches() ? path : "unknown";
    }
}
