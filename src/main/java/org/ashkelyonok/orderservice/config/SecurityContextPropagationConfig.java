package org.ashkelyonok.orderservice.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
@Configuration
public class SecurityContextPropagationConfig {

    @PostConstruct
    public void enableSecurityContextPropagation() {
        log.info("Setting SecurityContextHolder strategy to MODE_INHERITABLETHREADLOCAL to support Feign Circuit Breakers");
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }
}
