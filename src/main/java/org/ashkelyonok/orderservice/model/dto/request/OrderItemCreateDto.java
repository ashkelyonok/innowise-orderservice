package org.ashkelyonok.orderservice.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO for adding an item to an order")
public class OrderItemCreateDto {

    @Schema(description = "ID of the item in the catalog",
            example = "105",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Item ID is required")
    private Long itemId;

    @Schema(description = "Quantity of the item",
            example = "2",
            minimum = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private Integer quantity;
}
