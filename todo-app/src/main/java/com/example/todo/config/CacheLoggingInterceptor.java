package com.example.todo.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * CacheLoggingInterceptor - Logs cache status before and after each request.
 * 
 * Captures cache stats at request start and end to determine if cache was used.
 */
@Component
@Log4j2
public class CacheLoggingInterceptor implements HandlerInterceptor {

    private final CacheManager cacheManager;
    private static final ThreadLocal<Long> requestHitCount = new ThreadLocal<>();
    private static final ThreadLocal<Long> requestMissCount = new ThreadLocal<>();

    public CacheLoggingInterceptor(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Skip non-API requests
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/") && !uri.equals("/")) {
            return true;
        }

        // Capture cache stats at request start
        long[] stats = getCacheStats();
        requestHitCount.set(stats[0]);
        requestMissCount.set(stats[1]);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception ex) {
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/") && !uri.equals("/")) {
            return;
        }

        Long startHits = requestHitCount.get();
        Long startMisses = requestMissCount.get();

        if (startHits == null || startMisses == null) {
            return;
        }

        long[] endStats = getCacheStats();
        long hitsThisRequest = endStats[0] - startHits;
        long missesThisRequest = endStats[1] - startMisses;

        if (hitsThisRequest > 0 || missesThisRequest > 0) {
            String source = hitsThisRequest > 0 && missesThisRequest == 0 ? "[CACHE HIT]"
                    : missesThisRequest > 0 && hitsThisRequest == 0 ? "[CACHE MISS]" : "[CACHE MIXED]";
            log.info("{} {} - Hits: {}, Misses: {}",
                    source, uri, hitsThisRequest, missesThisRequest);
        }

        // Cleanup
        requestHitCount.remove();
        requestMissCount.remove();
    }

    private long[] getCacheStats() {
        long totalHits = 0;
        long totalMisses = 0;

        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache instanceof org.springframework.cache.caffeine.CaffeineCache caffeineCache) {
                var stats = caffeineCache.getNativeCache().stats();
                totalHits += stats.hitCount();
                totalMisses += stats.missCount();
            }
        }

        return new long[] { totalHits, totalMisses };
    }
}
