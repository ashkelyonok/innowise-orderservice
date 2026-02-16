package org.ashkelyonok.orderservice.client.fallback;

import lombok.extern.slf4j.Slf4j;
import org.ashkelyonok.orderservice.client.UserServiceClient;
import org.ashkelyonok.orderservice.model.dto.response.UserResponseDto;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserServiceFallback implements UserServiceClient {

    @Override
    public UserResponseDto getUserById(Long id) {
        log.warn("User Service fallback triggered for ID: {}", id);
        return UserResponseDto.builder()
                .id(id)
                .name("Unknown")
                .surname("Service Unavailable")
                .email("unavailable@test.com")
                .build();
    }

    @Override
    public UserResponseDto getUserByEmail(String email) {
        log.warn("User Service fallback triggered for email: {}", email);
        return UserResponseDto.builder()
                .id(0L)
                .name("Unknown")
                .surname("Service Unavailable")
                .email(email)
                .build();
    }
}