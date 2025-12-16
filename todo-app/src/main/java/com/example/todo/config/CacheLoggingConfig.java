package com.example.todo.config;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * CacheLoggingConfig - Logs cache statistics for monitoring cache hit/miss
 * rates.
 * 
 * Logs are output every 30 seconds showing:
 * - Hit count
 * - Miss count
 * - Hit rate percentage
 * - Eviction count
 */
@Configuration
@Log4j2
public class CacheLoggingConfig {

    private final CacheManager cacheManager;

    public CacheLoggingConfig(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Scheduled(fixedRate = 30000) // Log every 30 seconds
    public void logCacheStats() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache caffeineCache) {
                Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
                var stats = nativeCache.stats();

                if (stats.hitCount() > 0 || stats.missCount() > 0) {
                    log.info("[CACHE STATS] {} - Hits: {}, Misses: {}, HitRate: {}%, Evictions: {}",
                            cacheName,
                            stats.hitCount(),
                            stats.missCount(),
                            String.format("%.1f", stats.hitRate() * 100),
                            stats.evictionCount());
                }
            }
        });
    }
}
