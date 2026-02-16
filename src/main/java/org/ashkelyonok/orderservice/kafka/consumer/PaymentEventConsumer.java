package org.ashkelyonok.orderservice.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ashkelyonok.orderservice.model.event.PaymentStatusEvent;
import org.ashkelyonok.orderservice.service.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final OrderService orderService;

    @KafkaListener(
            topics = "${application.kafka.topic.payment-status}",
            groupId = "${application.kafka.consumer-group-id}",
            containerFactory = "kafkaListenerContainerFactory" // Points to bean in KafkaConfig
    )
    public void consumePaymentStatusEvent(PaymentStatusEvent event) {
        log.info("Received PaymentStatusEvent: {}", event);
        try {
            orderService.updateOrderStatusByPayment(
                    event.getOrderId(),
                    event.getStatus()
            );
        } catch (Exception e) {
            log.error("Error processing payment event for orderId: {}", event.getOrderId(), e);
        }
    }
}
