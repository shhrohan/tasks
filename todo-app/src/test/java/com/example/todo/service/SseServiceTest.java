package com.example.todo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.*;

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
}
