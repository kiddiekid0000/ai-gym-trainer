package com.aigymtrainer.backend.auth.mapper;

import com.aigymtrainer.backend.auth.dto.AuthResponse;
import com.aigymtrainer.backend.user.domain.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthMapper {
    
    @Mapping(source = "id", target = "id")
    @Mapping(source = "role", target = "role")
    @Mapping(source = "status", target = "status")
    AuthResponse toAuthResponse(User user);
}
