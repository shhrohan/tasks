package com.example.todo.aspect;

import com.example.todo.annotation.Idempotent;
import com.example.todo.exception.DuplicateOperationException;
import com.example.todo.service.IdempotencyService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class IdempotencyAspectTest {

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private Idempotent idempotent;

    private IdempotencyAspect idempotencyAspect;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        idempotencyAspect = new IdempotencyAspect(idempotencyService);
    }

    @Test
    void testCheckIdempotency_Success() throws Throwable {
        when(idempotent.keyExpression()).thenReturn("'testKey'");
        when(idempotent.windowSeconds()).thenReturn(5);
        when(idempotencyService.isDuplicate(anyString(), anyInt())).thenReturn(false);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(this.getClass().getDeclaredMethod("mockMethod", String.class));
        when(joinPoint.getArgs()).thenReturn(new Object[] { "arg1" });
        when(joinPoint.proceed()).thenReturn("success");

        Object result = idempotencyAspect.checkIdempotency(joinPoint, idempotent);

        assertEquals("success", result);
        verify(idempotencyService).isDuplicate(eq("testKey"), eq(5L));
        verify(idempotencyService).complete("testKey");
    }

    @Test
    void testCheckIdempotency_Duplicate() throws Throwable {
        when(idempotent.keyExpression()).thenReturn("'testKey'");
        when(idempotent.windowSeconds()).thenReturn(5);
        when(idempotencyService.isDuplicate(anyString(), anyLong())).thenReturn(true);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(this.getClass().getDeclaredMethod("mockMethod", String.class));
        when(joinPoint.getArgs()).thenReturn(new Object[] { "arg1" });

        assertThrows(DuplicateOperationException.class, () -> {
            idempotencyAspect.checkIdempotency(joinPoint, idempotent);
        });

        verify(idempotencyService).isDuplicate(eq("testKey"), eq(5L));
        verify(idempotencyService, never()).complete(anyString());
    }

    @Test
    void testCheckIdempotency_Exception() throws Throwable {
        when(idempotent.keyExpression()).thenReturn("'testKey'");
        when(idempotencyService.isDuplicate(anyString(), anyInt())).thenReturn(false);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(this.getClass().getDeclaredMethod("mockMethod", String.class));
        when(joinPoint.getArgs()).thenReturn(new Object[] { "arg1" });
        when(joinPoint.proceed()).thenThrow(new RuntimeException("Error"));

        assertThrows(RuntimeException.class, () -> {
            idempotencyAspect.checkIdempotency(joinPoint, idempotent);
        });

        verify(idempotencyService).complete("testKey");
    }

    @Test
    void testCheckIdempotency_SpelFallback() throws Throwable {
        when(idempotent.keyExpression()).thenReturn("invalid - expression");
        when(idempotent.windowSeconds()).thenReturn(5);
        when(idempotencyService.isDuplicate(anyString(), anyInt())).thenReturn(false);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        Method method = this.getClass().getDeclaredMethod("mockMethod", String.class);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[] { "arg1" });
        when(joinPoint.proceed()).thenReturn("success");

        Object result = idempotencyAspect.checkIdempotency(joinPoint, idempotent);

        assertNotNull(result);
        verify(idempotencyService).isDuplicate(contains("mockMethod"), anyLong());
    }

    @Test
    void testCheckIdempotency_NullKeyExpression() throws Throwable {
        when(idempotent.keyExpression()).thenReturn("null");
        when(idempotencyService.isDuplicate(anyString(), anyLong())).thenReturn(false);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(this.getClass().getDeclaredMethod("mockMethod", String.class));
        when(joinPoint.getArgs()).thenReturn(new Object[] { "arg1" });

        idempotencyAspect.checkIdempotency(joinPoint, idempotent);

        verify(idempotencyService).isDuplicate(eq("null"), anyLong());
    }

    void mockMethod(String arg) {
    }
}
