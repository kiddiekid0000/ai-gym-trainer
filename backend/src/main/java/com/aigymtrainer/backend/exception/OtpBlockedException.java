package com.aigymtrainer.backend.exception;

public class OtpBlockedException extends BaseException {
    public OtpBlockedException(String email) {
        super("OTP_BLOCKED", "Too many failed OTP attempts for '" + email + "'. Please try again later");
    }
}
