package org.ashkelyonok.orderservice.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO for item details within an order")
public class OrderItemResponseDto {

    @Schema(description = "ID of the order-item record",
            example = "500")
    private Long id;

    @Schema(description = "ID of the original item",
            example = "105")
    private Long itemId;

    @Schema(description = "Name of the item (snapshot)",
            example = "Wireless Mouse")
    private String name;

    @Schema(description = "Price per unit at purchase time",
            example = "29.99")
    private BigDecimal price;

    @Schema(description = "Quantity purchased",
            example = "2")
    private Integer quantity;

    @Schema(description = "Total price for this line item (price * quantity)",
            example = "59.98")
    private BigDecimal totalItemPrice;
}