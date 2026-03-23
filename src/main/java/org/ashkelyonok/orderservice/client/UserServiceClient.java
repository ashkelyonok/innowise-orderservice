package org.ashkelyonok.orderservice.client;

import org.ashkelyonok.orderservice.client.fallback.UserServiceFallbackFactory;
import org.ashkelyonok.orderservice.config.FeignConfig;
import org.ashkelyonok.orderservice.model.dto.response.UserPageResponse;
import org.ashkelyonok.orderservice.model.dto.response.UserResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Set;

@FeignClient(
        name = "user-service",
        url = "${application.config.user-service-url}",
        fallbackFactory = UserServiceFallbackFactory.class,
        configuration = FeignConfig.class
)
public interface UserServiceClient {

    @GetMapping("/api/v1/users/{id}")
    UserResponseDto getUserById(@PathVariable("id") Long id);

    @GetMapping("/api/v1/users")
    UserPageResponse getUserByEmail(@RequestParam("email") String email);

    @GetMapping("/api/v1/users")
    UserPageResponse getUsersByIds(@RequestParam("ids") Set<Long> ids);
}
