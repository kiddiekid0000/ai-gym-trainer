package com.aigymtrainer.backend.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiError> handleDuplicateEmailException(DuplicateEmailException e, HttpServletRequest request) {
        return buildApiErrorResponse(HttpStatus.CONFLICT, e, request);
    }

    @ExceptionHandler(AccountNotVerifiedException.class)
    public ResponseEntity<ApiError> handleAccountNotVerifiedException(AccountNotVerifiedException e, HttpServletRequest request) {
        return buildApiErrorResponse(HttpStatus.FORBIDDEN, e, request);
    }

    @ExceptionHandler(AccountSuspendedException.class)
    public ResponseEntity<ApiError> handleAccountSuspendedException(AccountSuspendedException e, HttpServletRequest request) {
        return buildApiErrorResponse(HttpStatus.FORBIDDEN, e, request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentialsException(InvalidCredentialsException e, HttpServletRequest request) {
        return buildApiErrorResponse(HttpStatus.UNAUTHORIZED, e, request);
    }

    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ApiError> handleInvalidOtpException(InvalidOtpException e, HttpServletRequest request) {
        return buildApiErrorResponse(HttpStatus.BAD_REQUEST, e, request);
    }

    @ExceptionHandler(OtpBlockedException.class)
    public ResponseEntity<ApiError> handleOtpBlockedException(OtpBlockedException e, HttpServletRequest request) {
        return buildApiErrorResponse(HttpStatus.TOO_MANY_REQUESTS, e, request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFoundException(UserNotFoundException e, HttpServletRequest request) {
        return buildApiErrorResponse(HttpStatus.NOT_FOUND, e, request);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiError> handleInvalidRefreshTokenException(InvalidRefreshTokenException e, HttpServletRequest request) {
        return buildApiErrorResponse(HttpStatus.UNAUTHORIZED, e, request);
    }

    @ExceptionHandler(TokenRevokedException.class)
    public ResponseEntity<ApiError> handleTokenRevokedException(TokenRevokedException e, HttpServletRequest request) {
        return buildApiErrorResponse(HttpStatus.UNAUTHORIZED, e, request);
    }

    @ExceptionHandler(TokenBlacklistException.class)
    public ResponseEntity<ApiError> handleTokenBlacklistException(TokenBlacklistException e, HttpServletRequest request) {
        return buildApiErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpServletRequest request) {
        Map<String, String> details = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error ->
                details.put(error.getField(), error.getDefaultMessage())
        );
        
        ApiError apiError = new ApiError(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Validation Failed",
            "Request validation failed",
            request.getRequestURI(),
            details
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        ApiError apiError = new ApiError(
            LocalDateTime.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            e.getMessage(),
            request.getRequestURI(),
            null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiError);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGlobalException(Exception e, HttpServletRequest request) {
        ApiError apiError = new ApiError(
            LocalDateTime.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "An unexpected error occurred",
            request.getRequestURI(),
            null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiError);
    }

    private ResponseEntity<ApiError> buildApiErrorResponse(HttpStatus status, BaseException e, HttpServletRequest request) {
        ApiError apiError = new ApiError(
            LocalDateTime.now(),
            status.value(),
            e.getCode(),
            e.getMessage(),
            request.getRequestURI(),
            null
        );
        return ResponseEntity.status(status).body(apiError);
    }
}

