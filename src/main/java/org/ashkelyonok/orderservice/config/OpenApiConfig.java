package org.ashkelyonok.orderservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Service API")
                        .version("1.0.0")
                        .description("Microservice for managing orders.")
                        .contact(new Contact()
                                .name("Anastasia Shkelyonok")
                                .email("anastasia.shkelyonok@gmail.com")))
                .servers(
                        List.of(new Server().url("http://localhost:8083").description("Development server")));
    }
}
