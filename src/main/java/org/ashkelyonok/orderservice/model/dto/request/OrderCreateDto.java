package org.ashkelyonok.orderservice.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO for creating a new order")
public class OrderCreateDto {

    @Schema(description = "Email of the user placing the order",
            example = "john@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "User email is required")
    @Email(message = "Invalid email format")
    private String userEmail;

    @Schema(description = "List of items to order",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "Order must contain at least one item")
    private List<OrderItemCreateDto> items;
}
