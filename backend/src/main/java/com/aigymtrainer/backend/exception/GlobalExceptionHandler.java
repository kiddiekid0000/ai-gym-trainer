package com.aigymtrainer.backend.exception;

import com.aigymtrainer.backend.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<?> handleDuplicateEmailException(DuplicateEmailException e) {
        return buildErrorResponse(HttpStatus.CONFLICT, e);
    }

    @ExceptionHandler(AccountNotVerifiedException.class)
    public ResponseEntity<?> handleAccountNotVerifiedException(AccountNotVerifiedException e) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, e);
    }

    @ExceptionHandler(AccountSuspendedException.class)
    public ResponseEntity<?> handleAccountSuspendedException(AccountSuspendedException e) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, e);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<?> handleInvalidCredentialsException(InvalidCredentialsException e) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, e);
    }

    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<?> handleInvalidOtpException(InvalidOtpException e) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, e);
    }

    @ExceptionHandler(OtpBlockedException.class)
    public ResponseEntity<?> handleOtpBlockedException(OtpBlockedException e) {
        return buildErrorResponse(HttpStatus.TOO_MANY_REQUESTS, e);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<?> handleUserNotFoundException(UserNotFoundException e) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, e);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<?> handleInvalidRefreshTokenException(InvalidRefreshTokenException e) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, e);
    }

    @ExceptionHandler(TokenRevokedException.class)
    public ResponseEntity<?> handleTokenRevokedException(TokenRevokedException e) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, e);
    }

    @ExceptionHandler(TokenBlacklistException.class)
    public ResponseEntity<?> handleTokenBlacklistException(TokenBlacklistException e) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Validation Failed");
        response.put("message", "Request validation failed");
        response.put("details", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Internal Server Error");
        response.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGlobalException(Exception e) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Internal Server Error");
        response.put("message", "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private ResponseEntity<?> buildErrorResponse(HttpStatus status, BaseException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", e.getCode());
        response.put("message", e.getMessage());
        return ResponseEntity.status(status).body(response);
    }
}

