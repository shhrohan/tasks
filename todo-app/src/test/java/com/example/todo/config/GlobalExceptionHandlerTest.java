package com.example.todo.config;

import com.example.todo.exception.DuplicateOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void testHandleDuplicateOperation() {
        DuplicateOperationException ex = new DuplicateOperationException("Dup", "key-123");
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleDuplicateOperation(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Conflict", response.getBody().get("error"));
        assertEquals("key-123", response.getBody().get("operationKey"));
    }

    @Test
    void testHandleDuplicateOperation_NoKey() {
        DuplicateOperationException ex = new DuplicateOperationException("Dup", null);
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleDuplicateOperation(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertFalse(response.getBody().containsKey("operationKey"));
    }

    @Test
    void testHandleIllegalArgument() {
        IllegalArgumentException ex = new IllegalArgumentException("Bad input");
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Bad input", response.getBody().get("message"));
    }
}
