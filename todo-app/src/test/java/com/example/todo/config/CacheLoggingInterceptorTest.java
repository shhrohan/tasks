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

    private CacheLoggingInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new CacheLoggingInterceptor(cacheManager);
    }

    @Test
    void preHandle_ShouldSkipNonApiRequests() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/css/style.css");

        // Act
        boolean result = interceptor.preHandle(request, response, new Object());

        // Assert
        assertTrue(result);
        verifyNoInteractions(cacheManager);
    }

    @Test
    void preHandle_ShouldReturnTrueForApiRequests() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/tasks");
        when(cacheManager.getCacheNames()).thenReturn(Collections.emptySet());

        // Act
        boolean result = interceptor.preHandle(request, response, new Object());

        // Assert
        assertTrue(result);
    }

    @Test
    void preHandle_ShouldReturnTrueForRootPath() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/");
        when(cacheManager.getCacheNames()).thenReturn(Collections.emptySet());

        // Act
        boolean result = interceptor.preHandle(request, response, new Object());

        // Assert
        assertTrue(result);
    }

    @Test
    void afterCompletion_ShouldSkipNonApiRequests() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/js/app.js");

        // Act
        interceptor.afterCompletion(request, response, new Object(), null);

        // Assert - Should complete without errors
        verifyNoInteractions(cacheManager);
    }

    @Test
    void afterCompletion_ShouldHandleNullStartStats() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/tasks");
        // Don't call preHandle, so start stats are null

        // Act - Should not throw
        interceptor.afterCompletion(request, response, new Object(), null);

        // Assert - Method completes without exception
    }

    @Test
    void preHandle_ShouldCaptureCacheStats_ForApiRequests() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/swimlanes");
        when(cacheManager.getCacheNames()).thenReturn(Collections.singleton("lanes"));
        when(cacheManager.getCache("lanes")).thenReturn(caffeineCache);
        when(caffeineCache.getNativeCache()).thenReturn(nativeCache);
        CacheStats stats = CacheStats.of(10, 5, 0, 0, 0, 0, 0);
        when(nativeCache.stats()).thenReturn(stats);

        // Act
        boolean result = interceptor.preHandle(request, response, new Object());

        // Assert
        assertTrue(result);
        verify(cacheManager).getCacheNames();
    }

    @Test
    void afterCompletion_ShouldLogCacheActivity_WhenStatsChanged() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/tasks");
        when(cacheManager.getCacheNames()).thenReturn(Collections.singleton("tasks"));
        when(cacheManager.getCache("tasks")).thenReturn(caffeineCache);
        when(caffeineCache.getNativeCache()).thenReturn(nativeCache);
        
        // First call (preHandle) - initial stats
        CacheStats initialStats = CacheStats.of(0, 0, 0, 0, 0, 0, 0);
        // Second call (afterCompletion) - updated stats
        CacheStats updatedStats = CacheStats.of(1, 0, 0, 0, 0, 0, 0);
        when(nativeCache.stats()).thenReturn(initialStats, updatedStats);

        // Act
        interceptor.preHandle(request, response, new Object());
        interceptor.afterCompletion(request, response, new Object(), null);

        // Assert - Method completes and stats are captured
        verify(cacheManager, atLeast(2)).getCacheNames();
    }
}
