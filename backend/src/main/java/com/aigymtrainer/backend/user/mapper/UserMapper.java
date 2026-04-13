package com.aigymtrainer.backend.user.mapper;

import com.aigymtrainer.backend.user.domain.User;
import com.aigymtrainer.backend.user.dto.UserResponseDto;
import com.aigymtrainer.backend.user.dto.UserProfileDto;
import com.aigymtrainer.backend.user.dto.AdminDataDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    
    UserResponseDto toResponseDto(User user);
    
    UserProfileDto toProfileDto(User user);
    
    AdminDataDto toAdminDataDto(User user);
}
