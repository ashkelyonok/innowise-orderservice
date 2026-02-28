package org.ashkelyonok.orderservice.model.event;

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
@Schema(description = "Event broadcast when a new order is successfully created in the system")
public class OrderCreatedEvent {

    @Schema(description = "Unique identifier of the newly created order",
            example = "1001",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long orderId;

    @Schema(description = "ID of the user who placed the order",
            example = "55",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;

    @Schema(description = "Total cost of the order to be processed for payment",
            example = "150.75",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal totalAmount;

    @Schema(description = "Exact timestamp when the order was finalized",
            example = "2023-10-27T10:15:30")
    private LocalDateTime createdAt;
}
