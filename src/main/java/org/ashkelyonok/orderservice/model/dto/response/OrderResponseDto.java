package org.ashkelyonok.orderservice.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ashkelyonok.orderservice.model.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO for order details")
public class OrderResponseDto {

    @Schema(description = "Unique order identifier",
            example = "1001")
    private Long id;

    @Schema(description = "Detailed user information (fetched from User Service)")
    private UserResponseDto userInfo;

    @Schema(description = "Payment ID (if paid)",
            example = "55501")
    private Long paymentId;

    @Schema(description = "Current status of the order",
            example = "CREATED")
    private OrderStatus status;

    @Schema(description = "Total price of the order",
            example = "59.98")
    private BigDecimal totalPrice;

    @Schema(description = "List of items in the order")
    private List<OrderItemResponseDto> items;

    @Schema(description = "Order creation timestamp",
            accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp",
            accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;
}