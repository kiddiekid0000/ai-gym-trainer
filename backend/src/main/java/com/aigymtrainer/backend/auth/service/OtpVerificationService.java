package com.aigymtrainer.backend.auth.service;

public interface OtpVerificationService {
    void sendOtp(String email);
    void verifyOtp(String email, String otp);
    void resendOtp(String email);
}
