package com.example.todo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class IdempotencyServiceTest {

    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        idempotencyService = new IdempotencyService();
    }

    @Test
    void testIsDuplicate_NewKey() {
        boolean duplicate = idempotencyService.isDuplicate("key1");
        assertFalse(duplicate);
        assertEquals(1, idempotencyService.getActiveOperationCount());
    }

    @Test
    void testIsDuplicate_SameKeyWithinWindow() {
        idempotencyService.isDuplicate("key1", 5);
        boolean duplicate = idempotencyService.isDuplicate("key1", 5);
        assertTrue(duplicate);
    }

    @Test
    void testIsDuplicate_SameKeyAfterWindow() throws InterruptedException {
        // We can't easily fake time without a Clock bean, but we can use a very short
        // window
        idempotencyService.isDuplicate("key1", 0); // Window of 0 seconds
        Thread.sleep(10); // Wait a tiny bit to ensure "now" has moved past
        boolean duplicate = idempotencyService.isDuplicate("key1", 0);
        assertFalse(duplicate, "Should not be duplicate after 0s window");
    }

    @Test
    void testComplete() {
        idempotencyService.isDuplicate("key1");
        idempotencyService.complete("key1");
        assertFalse(idempotencyService.isDuplicate("key1"));
    }

    @Test
    void testCleanup() throws Exception {
        idempotencyService.isDuplicate("fresh", 100);
        idempotencyService.isDuplicate("expired", 100);

        // Use reflection to backdate "expired"
        java.lang.reflect.Field field = IdempotencyService.class.getDeclaredField("recentOperations");
        field.setAccessible(true);
        java.util.concurrent.ConcurrentHashMap<String, Instant> map = (java.util.concurrent.ConcurrentHashMap<String, Instant>) field
                .get(idempotencyService);

        map.put("expired", Instant.now().minusSeconds(200));

        // isDuplicate calls cleanup()
        idempotencyService.isDuplicate("trigger", 100);

        // The "expired" one should be gone, "fresh" and "trigger" should remain
        assertEquals(2, idempotencyService.getActiveOperationCount());
    }
}
