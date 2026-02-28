package org.ashkelyonok.orderservice.model.event;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Event received regarding the payment outcome of a specific order")
public class PaymentStatusEvent {

    @Schema(description = "The Order ID this payment update belongs to",
            example = "1001",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long orderId;

    @Schema(description = "The status of the payment transaction",
            example = "SUCCESS",
            allowableValues = {"SUCCESS", "FAILED", "PENDING"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    @Schema(description = "External Payment ID (from payment provider like Stripe/PayPal)",
            example = "pay_12345abcde")
    private String paymentId;

    @Schema(description = "Timestamp when the payment was processed",
            example = "2023-10-27T10:16:00")
    private LocalDateTime timestamp;
}
