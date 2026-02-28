package org.ashkelyonok.orderservice.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO for updating catalog item details")
public class ItemUpdateDto {

    @Schema(description = "Updated name of the item",
            example = "Gaming Mouse")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Schema(description = "Updated price",
            example = "25.50")
    @Positive(message = "Price must be positive")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;
}
