package org.ashkelyonok.orderservice.model.dto.error;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard Error Response")
public class ErrorResponseDto {

    @Schema(description = "Error timestamp", example = "2025-01-15T10:07:00")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP status code", example = "404")
    private int status;

    @Schema(description = "Error type", example = "Not Found")
    private String error;

    @Schema(description = "Detailed error message", example = "Order with id 1 not found")
    private String message;

    @Schema(description = "Request path", example = "/api/v1/orders/1")
    private String path;
}