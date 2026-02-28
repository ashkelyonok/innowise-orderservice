package org.ashkelyonok.orderservice.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO for user information fetched from User Service")
public class UserResponseDto {

    @Schema(description = "Unique user identifier",
            example = "1",
            accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "User's first name",
            example = "John")
    private String name;

    @Schema(description = "User's surname",
            example = "Doe")
    private String surname;

    @Schema(description = "User's email",
            example = "john.doe@example.com")
    private String email;
}