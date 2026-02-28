package org.ashkelyonok.orderservice.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO for item details")
public class ItemResponseDto {

    @Schema(description = "Unique item identifier",
            example = "105")
    private Long id;

    @Schema(description = "Name of the item",
            example = "Wireless Mouse")
    private String name;

    @Schema(description = "Current price of the item",
            example = "29.99")
    private BigDecimal price;

    @Schema(description = "Record creation timestamp",
            accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp",
            accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

}