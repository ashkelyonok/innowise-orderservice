package org.ashkelyonok.orderservice.client.fallback;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.ashkelyonok.orderservice.client.UserServiceClient;
import org.ashkelyonok.orderservice.model.dto.response.UserPageResponse;
import org.ashkelyonok.orderservice.model.dto.response.UserResponseDto;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
public class UserServiceFallbackFactory implements FallbackFactory<UserServiceClient> {

    @Override
    public UserServiceClient create(Throwable cause) {
        return new UserServiceClient() {

            @Override
            public UserResponseDto getUserById(Long id) {
                if (shouldPropagateError(cause)) throw (RuntimeException) cause;

                log.warn("User Service fallback triggered for ID: {} due to: {}", id, cause.getMessage());
                return UserResponseDto.builder()
                        .id(id)
                        .name("Unknown")
                        .surname("Service Unavailable")
                        .email("unavailable@test.com")
                        .build();
            }

            @Override
            public UserPageResponse getUserByEmail(String email) {
                if (shouldPropagateError(cause)) throw (RuntimeException) cause;

                log.warn("User Service fallback triggered for email: {} due to: {}", email, cause.getMessage());
                return null;
            }

            @Override
            public UserPageResponse getUsersByIds(Set<Long> ids) {
                if (shouldPropagateError(cause)) throw (RuntimeException) cause;

                log.warn("User Service fallback triggered for batch IDs: {} due to: {}", ids, cause.getMessage());
                return null;
            }

            private boolean shouldPropagateError(Throwable cause) {
                return cause instanceof FeignException.Forbidden ||
                        cause instanceof FeignException.NotFound ||
                        cause instanceof FeignException.BadRequest;
            }
        };
    }
}