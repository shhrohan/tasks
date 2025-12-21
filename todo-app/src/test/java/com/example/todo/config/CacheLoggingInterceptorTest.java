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

        interceptor.afterCompletion(request, response, new Object(), null);

        verify(cacheManager).getCacheNames();
    }

    @Test
    void afterCompletion_WithNoCaches() {
        when(request.getRequestURI()).thenReturn("/api/tasks");
        when(cacheManager.getCacheNames()).thenReturn(Collections.emptyList());

        // Need to call preHandle to set up ThreadLocal, otherwise afterCompletion
        // returns early
        interceptor.preHandle(request, response, new Object());

        interceptor.afterCompletion(request, response, new Object(), null);

        verify(cacheManager, atLeastOnce()).getCacheNames();
    }
}
