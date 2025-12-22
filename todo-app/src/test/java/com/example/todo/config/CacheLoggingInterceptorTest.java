package com.example.todo.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class CacheLoggingInterceptorTest {

        @Mock
        private CacheManager cacheManager;

        @Mock
        private Cache cache;

        @Mock
        private HttpServletRequest request;

        @Mock
        private HttpServletResponse response;

        private CacheLoggingInterceptor interceptor;

        @BeforeEach
        void setUp() {
                MockitoAnnotations.openMocks(this);
                interceptor = new CacheLoggingInterceptor(cacheManager);
        }

        @Test
        void preHandle_ShouldNotThrow() {
                when(request.getRequestURI()).thenReturn("/api/tasks");
                when(request.getMethod()).thenReturn("GET");
                assertTrue(interceptor.preHandle(request, response, new Object()));
        }

        @Test
        void afterCompletion_ShouldLogStats() {
                when(request.getRequestURI()).thenReturn("/api/tasks");
                when(request.getMethod()).thenReturn("GET");
                when(cacheManager.getCacheNames()).thenReturn(Collections.singletonList("tasks"));
                when(cacheManager.getCache("tasks")).thenReturn(cache);

                // Mock the native cache if possible, but caffeine might not be present in unit
                // test classpath
                // The interceptor handles it gracefully if not caffeine.
                when(cache.getNativeCache()).thenReturn(new Object());

                // Need to call preHandle to set up ThreadLocal
                interceptor.preHandle(request, response, new Object());

                interceptor.afterCompletion(request, response, new Object(), null);

                verify(cacheManager, atLeastOnce()).getCacheNames();
        }

        @Test
        void afterCompletion_ShouldLogCacheMiss() {
                when(request.getRequestURI()).thenReturn("/api/tasks");
                when(request.getMethod()).thenReturn("GET");
                when(cacheManager.getCacheNames()).thenReturn(Collections.singletonList("tasks"));
                when(cacheManager.getCache("tasks")).thenReturn(cache);

                // Native cache stats mocking
                com.github.benmanes.caffeine.cache.stats.CacheStats stats = mock(
                                com.github.benmanes.caffeine.cache.stats.CacheStats.class);
                when(stats.hitCount()).thenReturn(0L);
                when(stats.missCount()).thenReturn(1L);

                com.github.benmanes.caffeine.cache.Cache nativeCache = mock(
                                com.github.benmanes.caffeine.cache.Cache.class);
                when(nativeCache.stats()).thenReturn(stats);

                org.springframework.cache.caffeine.CaffeineCache caffeineCache = mock(
                                org.springframework.cache.caffeine.CaffeineCache.class);
                when(caffeineCache.getNativeCache()).thenReturn(nativeCache);
                when(cacheManager.getCache("tasks")).thenReturn(caffeineCache);

                interceptor.preHandle(request, response, new Object());

                // Simulate change in stats
                com.github.benmanes.caffeine.cache.stats.CacheStats newStats = mock(
                                com.github.benmanes.caffeine.cache.stats.CacheStats.class);
                when(newStats.hitCount()).thenReturn(0L);
                when(newStats.missCount()).thenReturn(2L);
                when(nativeCache.stats()).thenReturn(newStats);

                interceptor.afterCompletion(request, response, new Object(), null);

                verify(cacheManager, atLeastOnce()).getCacheNames();
        }

        @Test
        void afterCompletion_ShouldLogCacheHit() {
                when(request.getRequestURI()).thenReturn("/api/tasks");
                when(cacheManager.getCacheNames()).thenReturn(Collections.singletonList("tasks"));

                com.github.benmanes.caffeine.cache.stats.CacheStats stats = mock(
                                com.github.benmanes.caffeine.cache.stats.CacheStats.class);
                when(stats.hitCount()).thenReturn(1L);
                when(stats.missCount()).thenReturn(0L);

                com.github.benmanes.caffeine.cache.Cache nativeCache = mock(
                                com.github.benmanes.caffeine.cache.Cache.class);
                when(nativeCache.stats()).thenReturn(stats);

                org.springframework.cache.caffeine.CaffeineCache caffeineCache = mock(
                                org.springframework.cache.caffeine.CaffeineCache.class);
                when(caffeineCache.getNativeCache()).thenReturn(nativeCache);
                when(cacheManager.getCache("tasks")).thenReturn(caffeineCache);

                interceptor.preHandle(request, response, new Object());

                com.github.benmanes.caffeine.cache.stats.CacheStats newStats = mock(
                                com.github.benmanes.caffeine.cache.stats.CacheStats.class);
                when(newStats.hitCount()).thenReturn(2L);
                when(newStats.missCount()).thenReturn(0L);
                when(nativeCache.stats()).thenReturn(newStats);

                interceptor.afterCompletion(request, response, new Object(), null);

                verify(cacheManager, atLeastOnce()).getCacheNames();
        }

        @Test
        void afterCompletion_ShouldLogMixed() {
                when(request.getRequestURI()).thenReturn("/api/tasks");
                when(cacheManager.getCacheNames()).thenReturn(Collections.singletonList("tasks"));

                com.github.benmanes.caffeine.cache.stats.CacheStats stats = mock(
                                com.github.benmanes.caffeine.cache.stats.CacheStats.class);
                when(stats.hitCount()).thenReturn(0L);
                when(stats.missCount()).thenReturn(0L);

                com.github.benmanes.caffeine.cache.Cache nativeCache = mock(
                                com.github.benmanes.caffeine.cache.Cache.class);
                when(nativeCache.stats()).thenReturn(stats);

                org.springframework.cache.caffeine.CaffeineCache caffeineCache = mock(
                                org.springframework.cache.caffeine.CaffeineCache.class);
                when(caffeineCache.getNativeCache()).thenReturn(nativeCache);
                when(cacheManager.getCache("tasks")).thenReturn(caffeineCache);

                interceptor.preHandle(request, response, new Object());

                com.github.benmanes.caffeine.cache.stats.CacheStats newStats = mock(
                                com.github.benmanes.caffeine.cache.stats.CacheStats.class);
                when(newStats.hitCount()).thenReturn(1L);
                when(newStats.missCount()).thenReturn(1L);
                when(nativeCache.stats()).thenReturn(newStats);

                interceptor.afterCompletion(request, response, new Object(), null);

                verify(cacheManager, atLeastOnce()).getCacheNames();
        }

        @Test
        void afterCompletion_ShouldReturnEarly_WhenNullStats() {
                when(request.getRequestURI()).thenReturn("/api/tasks");

                // Skip preHandle to leave logic null
                interceptor.afterCompletion(request, response, new Object(), null);

                // Assert nothing happened
                verify(cacheManager, never()).getCacheNames();
        }
}
