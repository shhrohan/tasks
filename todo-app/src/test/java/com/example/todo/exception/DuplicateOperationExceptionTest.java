package com.example.todo.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DuplicateOperationExceptionTest {

    @Test
    void shouldStoreMessage() {
        DuplicateOperationException ex = new DuplicateOperationException("Error message");
        assertEquals("Error message", ex.getMessage());
        assertNull(ex.getOperationKey());
    }

    @Test
    void shouldStoreMessageAndKey() {
        DuplicateOperationException ex = new DuplicateOperationException("Error message", "key123");
        assertEquals("Error message", ex.getMessage());
        assertEquals("key123", ex.getOperationKey());
    }
}
