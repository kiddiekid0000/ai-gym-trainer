// com.aigymtrainer.backend.config.GlobalExceptionHandler.java
package com.aigymtrainer.backend.config;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException e) {
        String message = e.getMessage();
        
        if ("Email already exists".equals(message)) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT) // 409 Conflict
                .body(Map.of("message", "Email already exists"));
        }
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of("message", message));
    }
}