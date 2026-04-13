package com.aigymtrainer.backend.user.mapper;

import com.aigymtrainer.backend.user.domain.User;
import com.aigymtrainer.backend.user.dto.UserDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    
    UserDto toUserDto(User user);
}
