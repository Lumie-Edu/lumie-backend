package com.lumie.common.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * In-process Caffeine cache for hot read-only lookups.
 *
 * <p><b>Why Caffeine, not Redis</b>: Lumie is a single Spring Boot monolith
 * scaled by replicating the same JAR. Per-pod Caffeine + short TTLs gets
 * 95% of the hit-rate benefit without paying the network round-trip and
 * adding Redis as a hard dependency for cache reads. Stale-window from a
 * sibling pod's evict is bounded by the TTL.
 *
 * <p>For cross-pod hard invalidation (e.g., tenant suspension must reflect
 * within seconds platform-wide) prefer publishing a domain event over
 * Modulith outbox; the listener calls {@link #CACHE_TENANTS_BY_SLUG}
 * eviction explicitly. Caffeine's expireAfterWrite is the long-stop, not
 * the propagation mechanism.
 *
 * <p>Caches:
 * <ul>
 *   <li>{@code tenants-by-slug} (5 min, 1k entries) — slug → TenantData
 *       reverse lookup hit on every authenticated request.
 *   <li>{@code admin-permissions} (1 min, 10k entries) — adminId → granted
 *       permission codes, hit on every authorization check.
 *   <li>{@code plans} (1 hr, 50 entries) — plan catalogue is near-static.
 *   <li>{@code exam-dashboard} (5 min, 500 entries) — tenantId → assembled
 *       dashboard statistics, computed directly from tenant-scoped SQL
 *       aggregates (COUNT/GROUP BY over exam_results/question_results).
 *       TTL-only invalidation — dashboard figures tolerate a few minutes of
 *       staleness, so there is no write-path eviction. One entry per tenant.
 * </ul>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_TENANTS_BY_SLUG = "tenants-by-slug";
    public static final String CACHE_ADMIN_PERMISSIONS = "admin-permissions";
    public static final String CACHE_PLANS = "plans";
    public static final String CACHE_EXAM_DASHBOARD = "exam-dashboard";

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                build(CACHE_TENANTS_BY_SLUG, Duration.ofMinutes(5), 1_000),
                build(CACHE_ADMIN_PERMISSIONS, Duration.ofMinutes(1), 10_000),
                build(CACHE_PLANS, Duration.ofHours(1), 50),
                build(CACHE_EXAM_DASHBOARD, Duration.ofMinutes(5), 500)
        ));
        return manager;
    }

    private CaffeineCache build(String name, Duration ttl, long maxSize) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .maximumSize(maxSize)
                .recordStats()
                .build());
    }
}
