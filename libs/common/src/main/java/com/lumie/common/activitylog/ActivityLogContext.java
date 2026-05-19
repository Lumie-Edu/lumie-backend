package com.lumie.common.activitylog;

/**
 * ThreadLocal flag to prevent duplicate activity logging.
 *
 * <p>When a service method annotated with {@link LogActivity} fires
 * {@code ActivityLogService.log(...)}, the {@link LogActivityAspect} sets
 * this flag so the fallback {@code ActivityLogInterceptor} skips the request.
 * Modules that have NOT yet been converted to {@code @LogActivity} rely on
 * the interceptor as before — this flag is a no-op for them until conversion.
 *
 * <p>Moved from {@code modules/activity-log/application/service} to
 * {@code libs/common/activitylog} so the AOP advice (also in libs/common)
 * can call {@link #markLogged()} without creating a cross-module dependency.
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
