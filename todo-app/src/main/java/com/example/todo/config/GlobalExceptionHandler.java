package com.example.todo.config;

import com.example.todo.exception.DuplicateOperationException;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler for REST controllers.
 * Transforms exceptions into appropriate HTTP responses.
 */
@RestControllerAdvice
@Log4j2
public class GlobalExceptionHandler {

    /**
     * Handle duplicate operation exceptions.
     * Returns 409 Conflict with error details.
     */
    @ExceptionHandler(DuplicateOperationException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateOperation(DuplicateOperationException ex) {
        log.warn("[ExceptionHandler] Duplicate operation: {}", ex.getOperationKey());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "Conflict");
        body.put("message", "Duplicate operation detected. Please wait a moment before retrying.");
        if (ex.getOperationKey() != null) {
            body.put("operationKey", ex.getOperationKey());
        }

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * Handle IllegalArgumentException.
     * Returns 400 Bad Request.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("[ExceptionHandler] Bad request: {}", ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
