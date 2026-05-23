package com.lumie.activitylog.application.dto.request;

/**
 * Client-reported Core Web Vital, emitted by the frontend via {@code navigator.sendBeacon}.
 * Fire-and-forget telemetry — all fields are normalized defensively when recorded, so a
 * malformed or partial beacon is bucketed rather than rejected.
 *
 * @param metric         web vital name (LCP, CLS, INP, FCP, TTFB)
 * @param value          measured value (ms for timing vitals, unitless score for CLS)
 * @param rating         "good" | "needs-improvement" | "poor"
 * @param route          normalized Next.js route template (e.g. /admin/students/[id]), may be null
 * @param navigationType "navigate" | "reload" | "back-forward" | "prerender", may be null
 */
public record WebVitalsReportRequest(
        String metric,
        double value,
        String rating,
        String route,
        String navigationType
) {}
