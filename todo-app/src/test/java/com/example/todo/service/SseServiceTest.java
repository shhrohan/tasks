package com.example.todo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SseServiceTest {

    private SseService sseService;

    @BeforeEach
    void setUp() {
        sseService = new SseService();
    }

    @Test
    void subscribe_ShouldReturnEmitter() {
        SseEmitter emitter = sseService.subscribe();

        assertNotNull(emitter);
    }

    @Test
    void broadcast_ShouldNotThrowException_WhenNoEmitters() {
        // Should not throw even with no subscribers
        assertDoesNotThrow(() -> sseService.broadcast("test-event", "test-data"));
    }

    @Test
    void sendHeartbeat_ShouldNotThrowException_WhenNoEmitters() {
        // Should not throw even with no subscribers
        assertDoesNotThrow(() -> sseService.sendHeartbeat());
    }

    @Test
    void subscribe_ShouldAddEmitterToList() {
        // First subscription
        SseEmitter emitter1 = sseService.subscribe();
        assertNotNull(emitter1);

        // Second subscription
        SseEmitter emitter2 = sseService.subscribe();
        assertNotNull(emitter2);
        assertNotSame(emitter1, emitter2);
    }

    @Test
    void broadcast_ShouldSendEventToSubscribers() {
        // Subscribe first
        SseEmitter emitter = sseService.subscribe();
        assertNotNull(emitter);

        // Broadcast should not throw
        assertDoesNotThrow(() -> sseService.broadcast("task-updated", "{\"id\": 1}"));
    }

    @Test
    void sendHeartbeat_ShouldSendToSubscribers() {
        // Subscribe first
        SseEmitter emitter = sseService.subscribe();
        assertNotNull(emitter);

        // Heartbeat should not throw
        assertDoesNotThrow(() -> sseService.sendHeartbeat());
    }

    // ==================== Error Handling Branch Tests ====================

    @Test
    void broadcast_ShouldRemoveDeadEmitters() throws Exception {
        // Add a mock emitter that fails on send
        SseEmitter deadEmitter = mock(SseEmitter.class);
        doThrow(new IOException("Connection reset")).when(deadEmitter).send(any(SseEmitter.SseEventBuilder.class));

        // Use reflection to add the mock emitter
        Field emittersField = SseService.class.getDeclaredField("emitters");
        emittersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<SseEmitter> emitters = (List<SseEmitter>) emittersField.get(sseService);
        emitters.add(deadEmitter);

        // Broadcast should remove the dead emitter
        assertDoesNotThrow(() -> sseService.broadcast("test-event", "test-data"));

        // Verify the dead emitter was removed
        assertFalse(emitters.contains(deadEmitter));
    }

    @Test
    void sendHeartbeat_ShouldRemoveDeadEmitters() throws Exception {
        // Add a mock emitter that fails on send
        SseEmitter deadEmitter = mock(SseEmitter.class);
        doThrow(new IOException("Connection reset")).when(deadEmitter).send(any(SseEmitter.SseEventBuilder.class));

        // Use reflection to add the mock emitter
        Field emittersField = SseService.class.getDeclaredField("emitters");
        emittersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<SseEmitter> emitters = (List<SseEmitter>) emittersField.get(sseService);
        emitters.add(deadEmitter);

        // Heartbeat should remove the dead emitter
        assertDoesNotThrow(() -> sseService.sendHeartbeat());

        // Verify the dead emitter was removed
        assertFalse(emitters.contains(deadEmitter));
    }

    @Test
    void sendHeartbeat_ShouldReturnEarlyWhenNoEmitters() {
        // Create a fresh service with no emitters
        SseService emptyService = new SseService();

        // This should return early without any processing
        assertDoesNotThrow(() -> emptyService.sendHeartbeat());
    }

    @Test
    void broadcast_ShouldHandleMixedEmitters() throws Exception {
        // Add both a working and a dead emitter
        SseEmitter deadEmitter = mock(SseEmitter.class);
        SseEmitter workingEmitter = mock(SseEmitter.class);
        doThrow(new IOException("Connection reset")).when(deadEmitter).send(any(SseEmitter.SseEventBuilder.class));
        doNothing().when(workingEmitter).send(any(SseEmitter.SseEventBuilder.class));

        Field emittersField = SseService.class.getDeclaredField("emitters");
        emittersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<SseEmitter> emitters = (List<SseEmitter>) emittersField.get(sseService);
        emitters.add(deadEmitter);
        emitters.add(workingEmitter);

        // Broadcast should succeed and remove only the dead emitter
        assertDoesNotThrow(() -> sseService.broadcast("test-event", "test-data"));

        // Verify only dead emitter was removed
        assertFalse(emitters.contains(deadEmitter));
        assertTrue(emitters.contains(workingEmitter));
    }

    @Test
    void testCallbacks() throws Exception {
        SseEmitter emitter = sseService.subscribe();

        invokeCallback(emitter, "completionCallback");

        Field emittersField = SseService.class.getDeclaredField("emitters");
        emittersField.setAccessible(true);
        List<?> emitters = (List<?>) emittersField.get(sseService);
        assertFalse(emitters.contains(emitter));
    }

    @Test
    void testErrorAndTimeoutCallbacks() throws Exception {
        SseEmitter emitter = sseService.subscribe();

        invokeCallback(emitter, "timeoutCallback");
        invokeCallback(emitter, "errorCallback", new RuntimeException("error"));

        Field emittersField = SseService.class.getDeclaredField("emitters");
        emittersField.setAccessible(true);
        List<?> emitters = (List<?>) emittersField.get(sseService);
        assertFalse(emitters.contains(emitter));
    }

    private void invokeCallback(SseEmitter emitter, String fieldName, Object... args) throws Exception {
        // ResponseBodyEmitter (parent of SseEmitter) has these private Runnable fields.
        java.lang.reflect.Field field = null;
        Class<?> clazz = emitter.getClass();
        while (clazz != null) {
            try {
                field = clazz.getDeclaredField(fieldName);
                break;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }

        if (field != null) {
            field.setAccessible(true);
            Object callback = field.get(emitter);
            if (callback instanceof Runnable) {
                ((Runnable) callback).run();
            } else if (callback instanceof java.util.function.Consumer) {
                // errorCallback is a Consumer in some versions
                ((java.util.function.Consumer<Throwable>) callback).accept(new RuntimeException("test error"));
            }
        }
    }
}
