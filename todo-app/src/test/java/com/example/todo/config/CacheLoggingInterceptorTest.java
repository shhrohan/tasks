package com.example.todo.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheLoggingInterceptorTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private CaffeineCache caffeineCache;

    @Mock
    private Cache<Object, Object> nativeCache;

    @Mock
    private org.springframework.cache.Cache nonCaffeineCache;

    private CacheLoggingInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new CacheLoggingInterceptor(cacheManager);
    }

    @Test
    void preHandle_ShouldSkipNonApiRequests() throws Exception {
        when(request.getRequestURI()).thenReturn("/css/style.css");
        boolean result = interceptor.preHandle(request, response, new Object());
        assertTrue(result);
        verifyNoInteractions(cacheManager);
    }

    @Test
    void preHandle_ShouldReturnTrueForApiRequests() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/tasks");
        when(cacheManager.getCacheNames()).thenReturn(Collections.emptySet());
        boolean result = interceptor.preHandle(request, response, new Object());
        assertTrue(result);
    }

    @Test
    void preHandle_ShouldReturnTrueForRootPath() throws Exception {
        when(request.getRequestURI()).thenReturn("/");
        when(cacheManager.getCacheNames()).thenReturn(Collections.emptySet());
        boolean result = interceptor.preHandle(request, response, new Object());
        assertTrue(result);
    }

    @Test
    void afterCompletion_ShouldSkipNonApiRequests() throws Exception {
        when(request.getRequestURI()).thenReturn("/js/app.js");
        interceptor.afterCompletion(request, response, new Object(), null);
        verifyNoInteractions(cacheManager);
    }

    @Test
    void afterCompletion_ShouldHandleNullStartStats() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/tasks");
        interceptor.afterCompletion(request, response, new Object(), null);
        // Should complete without exception
    }

    @Test
    void preHandle_ShouldCaptureCacheStats_ForApiRequests() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/swimlanes");
        when(cacheManager.getCacheNames()).thenReturn(Collections.singleton("lanes"));
        when(cacheManager.getCache("lanes")).thenReturn(caffeineCache);
        when(caffeineCache.getNativeCache()).thenReturn(nativeCache);
        CacheStats stats = CacheStats.of(10, 5, 0, 0, 0, 0, 0);
        when(nativeCache.stats()).thenReturn(stats);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verify(cacheManager).getCacheNames();
    }

    @Test
    void afterCompletion_ShouldLogCacheHit_WhenOnlyHits() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/tasks");
        when(cacheManager.getCacheNames()).thenReturn(Collections.singleton("tasks"));
        when(cacheManager.getCache("tasks")).thenReturn(caffeineCache);
        when(caffeineCache.getNativeCache()).thenReturn(nativeCache);
        
        // Initial: 0 hits, 0 misses; After: 1 hit, 0 misses (pure hit)
        CacheStats initialStats = CacheStats.of(0, 0, 0, 0, 0, 0, 0);
        CacheStats afterStats = CacheStats.of(1, 0, 0, 0, 0, 0, 0);
        when(nativeCache.stats()).thenReturn(initialStats, afterStats);

        interceptor.preHandle(request, response, new Object());
        interceptor.afterCompletion(request, response, new Object(), null);

        verify(cacheManager, atLeast(2)).getCacheNames();
    }

    @Test
    void afterCompletion_ShouldLogCacheMiss_WhenOnlyMisses() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/tasks");
        when(cacheManager.getCacheNames()).thenReturn(Collections.singleton("tasks"));
        when(cacheManager.getCache("tasks")).thenReturn(caffeineCache);
        when(caffeineCache.getNativeCache()).thenReturn(nativeCache);
        
        // Initial: 0 hits, 0 misses; After: 0 hits, 1 miss (pure miss)
        CacheStats initialStats = CacheStats.of(0, 0, 0, 0, 0, 0, 0);
        CacheStats afterStats = CacheStats.of(0, 1, 0, 0, 0, 0, 0);
        when(nativeCache.stats()).thenReturn(initialStats, afterStats);

        interceptor.preHandle(request, response, new Object());
        interceptor.afterCompletion(request, response, new Object(), null);

        verify(cacheManager, atLeast(2)).getCacheNames();
    }

    @Test
    void afterCompletion_ShouldLogCacheMixed_WhenHitsAndMisses() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/tasks");
        when(cacheManager.getCacheNames()).thenReturn(Collections.singleton("tasks"));
        when(cacheManager.getCache("tasks")).thenReturn(caffeineCache);
        when(caffeineCache.getNativeCache()).thenReturn(nativeCache);
        
        // Initial: 0 hits, 0 misses; After: 1 hit, 1 miss (mixed)
        CacheStats initialStats = CacheStats.of(0, 0, 0, 0, 0, 0, 0);
        CacheStats afterStats = CacheStats.of(1, 1, 0, 0, 0, 0, 0);
        when(nativeCache.stats()).thenReturn(initialStats, afterStats);

        interceptor.preHandle(request, response, new Object());
        interceptor.afterCompletion(request, response, new Object(), null);

        verify(cacheManager, atLeast(2)).getCacheNames();
    }

    @Test
    void getCacheStats_ShouldSkipNonCaffeineCaches() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/tasks");
        when(cacheManager.getCacheNames()).thenReturn(Collections.singleton("other"));
        when(cacheManager.getCache("other")).thenReturn(nonCaffeineCache);
        
        boolean result = interceptor.preHandle(request, response, new Object());
        
        assertTrue(result);
        verify(cacheManager).getCache("other");
    }

    @Test
    void afterCompletion_ShouldNotLog_WhenNoActivity() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/tasks");
        when(cacheManager.getCacheNames()).thenReturn(Collections.singleton("tasks"));
        when(cacheManager.getCache("tasks")).thenReturn(caffeineCache);
        when(caffeineCache.getNativeCache()).thenReturn(nativeCache);
        
        // Same stats before and after (no change)
        CacheStats stats = CacheStats.of(5, 5, 0, 0, 0, 0, 0);
        when(nativeCache.stats()).thenReturn(stats);

        interceptor.preHandle(request, response, new Object());
        interceptor.afterCompletion(request, response, new Object(), null);

        // Completes without logging since no new activity
        verify(cacheManager, atLeast(2)).getCacheNames();
    }

    @Test
    void getCacheStats_ShouldHandleNullCache() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/tasks");
        when(cacheManager.getCacheNames()).thenReturn(Collections.singleton("tasks"));
        when(cacheManager.getCache("tasks")).thenReturn(null);
        
        boolean result = interceptor.preHandle(request, response, new Object());
        
        assertTrue(result);
    }
}

