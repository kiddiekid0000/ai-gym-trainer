package com.aigymtrainer.backend.common.constant;

public class AuthConstants {
    
    // OTP Configuration
    public static final int OTP_LENGTH = 6;
    public static final int OTP_TTL_MINUTES = 10;
    public static final int OTP_MAX_FAILED_ATTEMPTS = 3;
    public static final int OTP_BLOCK_DURATION_HOURS = 1;
    public static final int OTP_MAX_RESEND_ATTEMPTS = 5;
    public static final int OTP_RESEND_WINDOW_SECONDS = 60;
    
    // JWT Configuration
    public static final long ACCESS_TOKEN_EXPIRATION_MS = 1000 * 60 * 15; // 15 minutes
    public static final long REFRESH_TOKEN_EXPIRATION_MS = 1000 * 60 * 60 * 24 * 7; // 7 days
    
    // Rate Limiting Configuration
    public static final long RATE_LIMIT_WINDOW_SECONDS = 60;
    public static final int IP_RATE_LIMIT = 10; // requests per minute per IP
    public static final int USER_RATE_LIMIT = 20; // requests per minute per user
    
    // Pending Account Cleanup
    public static final int PENDING_ACCOUNT_CLEANUP_HOURS = 24;
}
