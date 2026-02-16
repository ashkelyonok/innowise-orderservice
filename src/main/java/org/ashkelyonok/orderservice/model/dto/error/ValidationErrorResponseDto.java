package org.ashkelyonok.orderservice.model.dto.error;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Error response for validation failures")
public class ValidationErrorResponseDto extends ErrorResponseDto {

    @Schema(description = "Map of field names and validation error messages",
            example = "{\"userId\": \"User ID is required\"}")
    private Map<String, String> validationErrors;
}