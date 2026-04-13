package com.aigymtrainer.backend.common.constant;

public class RedisKeyConstants {
    
    // OTP Keys
    public static final String OTP_PREFIX = "otp:";
    public static final String OTP_FAILED_PREFIX = "otp_failed:";
    
    // Rate Limiting Keys
    public static final String RATE_LIMIT_IP_PREFIX = "rate_limit:ip:";
    public static final String RATE_LIMIT_USER_PREFIX = "rate_limit:user:";
    public static final String RATE_LIMIT_OTP_RESEND_PREFIX = "otp_resend:";
    
    // Token Keys
    public static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    public static final String BLACKLIST_PREFIX = "blacklist:";
    
    public static String otpKey(String email) {
        return OTP_PREFIX + email;
    }
    
    public static String otpFailedKey(String email) {
        return OTP_FAILED_PREFIX + email;
    }
    
    public static String ipRateLimitKey(String ip) {
        return RATE_LIMIT_IP_PREFIX + ip;
    }
    
    public static String userRateLimitKey(String email) {
        return RATE_LIMIT_USER_PREFIX + email;
    }
    
    public static String otpResendLimitKey(String email) {
        return RATE_LIMIT_OTP_RESEND_PREFIX + email;
    }
    
    public static String refreshTokenKey(String email) {
        return REFRESH_TOKEN_PREFIX + email;
    }
    
    public static String blacklistKey(String token) {
        return BLACKLIST_PREFIX + token;
    }
}
