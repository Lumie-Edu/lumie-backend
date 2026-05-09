package com.lumie.activitylog.application.service;

/**
 * ThreadLocal flag to prevent duplicate activity logging.
 * When a service manually logs an activity (with richer data like entity names),
 * it sets this flag so the auto-logging filter skips the request.
 */
public final class ActivityLogContext {

    private static final ThreadLocal<Boolean> LOGGED = ThreadLocal.withInitial(() -> false);

    private ActivityLogContext() {
    }

    public static void markLogged() {
        LOGGED.set(true);
    }

    public static boolean isLogged() {
        return LOGGED.get();
    }

    public static void clear() {
        LOGGED.remove();
    }
}
