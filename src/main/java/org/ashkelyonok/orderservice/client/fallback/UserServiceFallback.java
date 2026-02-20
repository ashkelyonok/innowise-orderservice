package org.ashkelyonok.orderservice.client.fallback;

import lombok.extern.slf4j.Slf4j;
import org.ashkelyonok.orderservice.client.UserServiceClient;
import org.ashkelyonok.orderservice.model.dto.response.UserPageResponse;
import org.ashkelyonok.orderservice.model.dto.response.UserResponseDto;
import org.springframework.stereotype.Component;

import java.util.Set;

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
    public UserPageResponse getUserByEmail(String email) {
        log.warn("User Service fallback triggered for email: {}", email);
        return null;
    }

    @Override
    public UserPageResponse getUsersByIds(Set<Long> ids) {
        log.warn("User Service fallback triggered for batch IDs: {}", ids);
        return null;
    }
}