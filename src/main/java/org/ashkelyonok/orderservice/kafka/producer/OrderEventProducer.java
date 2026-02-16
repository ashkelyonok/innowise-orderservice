package org.ashkelyonok.orderservice.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ashkelyonok.orderservice.model.event.OrderCreatedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${application.kafka.topic.order-created}")
    private String orderCreatedTopic;

    public void sendOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Sending OrderCreatedEvent to topic '{}': {}", orderCreatedTopic, event);

        kafkaTemplate.send(orderCreatedTopic, String.valueOf(event.getOrderId()), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.debug("Event sent successfully for orderId: {}", event.getOrderId());
                    } else {
                        log.error("Failed to send event for orderId: {}", event.getOrderId(), ex);
                    }
                });
    }
}
