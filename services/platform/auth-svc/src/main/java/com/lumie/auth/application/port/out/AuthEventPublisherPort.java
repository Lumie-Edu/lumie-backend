package com.lumie.auth.application.port.out;

/**
 * Output port for publishing authentication events.
 */
public interface AuthEventPublisherPort {

    /**
     * Publishes a user login event.
     *
     * @param userId the user ID
     * @param tenantSlug the tenant slug
     * @param email the user email
     * @param provider the authentication provider (local, google, kakao)
     */
    void publishLoginEvent(Long userId, String tenantSlug, String email, String provider);

    /**
     * Publishes a user logout event.
     *
     * @param userId the user ID
     * @param tenantSlug the tenant slug
     * @param logoutAll whether all sessions were logged out
     */
    void publishLogoutEvent(Long userId, String tenantSlug, boolean logoutAll);
}
