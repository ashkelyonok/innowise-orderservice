package org.ashkelyonok.orderservice.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ashkelyonok.orderservice.model.enums.OrderStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO for updating order status")
public class OrderUpdateStatusDto {

    @Schema(description = "New status for the order", example = "SHIPPED", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Status is required")
    private OrderStatus status;
}
