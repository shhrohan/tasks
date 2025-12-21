package com.example.todo.exception;

/**
 * Exception thrown when a duplicate operation is detected within the
 * idempotency window.
 * This typically results in a 409 Conflict HTTP response.
 */
public class DuplicateOperationException extends RuntimeException {

    private final String operationKey;

    public DuplicateOperationException(String message) {
        super(message);
        this.operationKey = null;
    }

    public DuplicateOperationException(String message, String operationKey) {
        super(message);
        this.operationKey = operationKey;
    }

    public String getOperationKey() {
        return operationKey;
    }
}
