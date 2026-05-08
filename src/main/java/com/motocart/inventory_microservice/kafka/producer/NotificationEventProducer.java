package com.motocart.inventory_microservice.kafka.producer;

import com.motocart.library.common.event.NotificationEvent;
import com.motocart.library.kafka.KafkaTopics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public NotificationEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendNotificationEvent(NotificationEvent event) {
        kafkaTemplate.send(KafkaTopics.NOTIFICATION_EVENTS, event);
    }
}
