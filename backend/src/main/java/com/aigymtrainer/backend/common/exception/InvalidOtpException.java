package com.aigymtrainer.backend.common.exception;

public class InvalidOtpException extends BaseException {
    public InvalidOtpException() {
        super("INVALID_OTP", "The OTP you provided is invalid");
    }
}
