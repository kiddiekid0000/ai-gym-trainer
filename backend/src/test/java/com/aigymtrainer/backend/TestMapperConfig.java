package com.aigymtrainer.backend;

import com.aigymtrainer.backend.auth.mapper.AuthMapper;
import com.aigymtrainer.backend.user.mapper.UserMapper;
import org.mapstruct.factory.Mappers;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestMapperConfig {

    @Bean
    @Primary
    public UserMapper userMapper() {
        return Mappers.getMapper(UserMapper.class);
    }

    @Bean
    @Primary
    public AuthMapper authMapper() {
        return Mappers.getMapper(AuthMapper.class);
    }
}
