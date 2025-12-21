package com.example.todo.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;

import java.util.Collections;
import java.util.Set;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheLoggingConfigTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private CaffeineCache caffeineCache;

    @Mock
    private Cache<Object, Object> nativeCache;

    @Mock
    private org.springframework.cache.Cache nonCaffeineCache;

    private CacheLoggingConfig config;

    @BeforeEach
    void setUp() {
        config = new CacheLoggingConfig(cacheManager);
    }

    @Test
    void logCacheStats_ShouldDoNothing_WhenNoCaches() {
        when(cacheManager.getCacheNames()).thenReturn(Collections.emptySet());

        config.logCacheStats();

        verify(cacheManager).getCacheNames();
    }

    @Test
    void logCacheStats_ShouldSkipNonCaffeineCaches() {
        when(cacheManager.getCacheNames()).thenReturn(Collections.singleton("simple"));
        when(cacheManager.getCache("simple")).thenReturn(nonCaffeineCache);

        config.logCacheStats();

        verify(cacheManager).getCache("simple");
    }

    @Test
    void logCacheStats_ShouldSkipLogging_WhenStatsAreZero() {
        when(cacheManager.getCacheNames()).thenReturn(Collections.singleton("tasks"));
        when(cacheManager.getCache("tasks")).thenReturn(caffeineCache);
        when(caffeineCache.getNativeCache()).thenReturn(nativeCache);
        
        CacheStats emptyStats = CacheStats.empty();
        when(nativeCache.stats()).thenReturn(emptyStats);

        config.logCacheStats();

        verify(nativeCache).stats();
        // Zero stats won't trigger logging, so we just verify it ran without error
    }

    @Test
    void logCacheStats_ShouldLog_WhenHitsExist() {
        when(cacheManager.getCacheNames()).thenReturn(Collections.singleton("tasks"));
        when(cacheManager.getCache("tasks")).thenReturn(caffeineCache);
        when(caffeineCache.getNativeCache()).thenReturn(nativeCache);
        
        CacheStats stats = CacheStats.of(10, 0, 0, 0, 0, 0, 0);
        when(nativeCache.stats()).thenReturn(stats);

        config.logCacheStats();

        verify(nativeCache).stats();
    }

    @Test
    void logCacheStats_ShouldLog_WhenMissesExist() {
        when(cacheManager.getCacheNames()).thenReturn(Collections.singleton("tasks"));
        when(cacheManager.getCache("tasks")).thenReturn(caffeineCache);
        when(caffeineCache.getNativeCache()).thenReturn(nativeCache);
        
        CacheStats stats = CacheStats.of(0, 5, 0, 0, 0, 0, 0);
        when(nativeCache.stats()).thenReturn(stats);

        config.logCacheStats();

        verify(nativeCache).stats();
    }
}
