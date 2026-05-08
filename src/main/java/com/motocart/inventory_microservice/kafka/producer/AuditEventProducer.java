package com.motocart.inventory_microservice.kafka.producer;

import com.motocart.library.common.event.AuditEvent;
import com.motocart.library.kafka.KafkaTopics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class AuditEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public AuditEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendAuditEvent(AuditEvent event) {
        kafkaTemplate.send(KafkaTopics.AUDIT_EVENTS, event);
    }
}
