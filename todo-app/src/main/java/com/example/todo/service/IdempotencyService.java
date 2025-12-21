package com.example.todo.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Idempotency service to prevent duplicate operations from rapid button clicks.
 * Uses a time-based key expiration approach with configurable window.
 * 
 * This is a lightweight, in-memory solution for single-instance deployments.
 * For multi-instance deployments, consider using Redis or database-backed
 * deduplication.
 */
@Service
@Log4j2
public class IdempotencyService {

    private final ConcurrentHashMap<String, Instant> recentOperations = new ConcurrentHashMap<>();
    private static final long DEFAULT_WINDOW_SECONDS = 5;

    /**
     * Check if an operation is a duplicate within the default 5-second window.
     * 
     * @param key Unique key for the operation
     * @return true if this is a duplicate (should be rejected), false if OK to
     *         proceed
     */
    public boolean isDuplicate(String key) {
        return isDuplicate(key, DEFAULT_WINDOW_SECONDS);
    }

    /**
     * Check if an operation is a duplicate within the specified time window.
     * Uses putIfAbsent for atomic check-and-set.
     * 
     * @param key           Unique key for the operation
     * @param windowSeconds Time window in seconds for duplicate detection
     * @return true if this is a duplicate (should be rejected), false if OK to
     *         proceed
     */
    public boolean isDuplicate(String key, long windowSeconds) {
        cleanup(windowSeconds);
        Instant now = Instant.now();
        Instant existing = recentOperations.putIfAbsent(key, now);

        if (existing != null) {
            // Check if existing entry is still within window
            if (existing.plusSeconds(windowSeconds).isAfter(now)) {
                log.warn("[IDEMPOTENCY] Duplicate operation detected: {}", key);
                return true;
            }
            // Existing entry expired, replace it
            recentOperations.put(key, now);
        }
        log.debug("[IDEMPOTENCY] Operation registered: {}", key);
        return false;
    }

    /**
     * Mark an operation as complete (remove from tracking).
     * Call this after successful completion to allow the same operation again.
     * 
     * @param key The same key used in isDuplicate()
     */
    public void complete(String key) {
        recentOperations.remove(key);
        log.debug("[IDEMPOTENCY] Operation completed and removed: {}", key);
    }

    /**
     * Clean up expired entries to prevent memory leaks.
     */
    private void cleanup(long windowSeconds) {
        Instant cutoff = Instant.now().minusSeconds(windowSeconds);
        int removed = 0;
        var iterator = recentOperations.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().isBefore(cutoff)) {
                iterator.remove();
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("[IDEMPOTENCY] Cleaned up {} expired entries", removed);
        }
    }

    /**
     * Get current count of tracked operations (for testing/monitoring).
     */
    public int getActiveOperationCount() {
        return recentOperations.size();
    }
}
