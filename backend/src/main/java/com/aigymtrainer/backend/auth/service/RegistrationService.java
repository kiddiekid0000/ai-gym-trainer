package com.aigymtrainer.backend.auth.service;

import com.aigymtrainer.backend.auth.dto.AuthResult;
import com.aigymtrainer.backend.user.dto.UserRegistrationDto;

public interface RegistrationService {
    AuthResult register(UserRegistrationDto userDto);
}
