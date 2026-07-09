package com.assistant.ai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        String message = ex.getMessage();

        // Map common messages to proper HTTP status codes
        if (message != null && (message.contains("not found") || message.contains("Not found"))) {
            return ResponseEntity.status(404).body(Map.of("message", message));
        }

        if (message != null && (message.contains("Access denied") || message.contains("Forbidden"))) {
            return ResponseEntity.status(403).body(Map.of("message", message));
        }

        return ResponseEntity.badRequest().body(Map.of("message", message != null ? message : "An unexpected error occurred"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        return ResponseEntity.status(500).body(Map.of("message", "Internal server error: " + ex.getMessage()));
    }
}
