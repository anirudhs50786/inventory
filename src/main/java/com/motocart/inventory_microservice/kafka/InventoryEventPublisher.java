package com.motocart.inventory_microservice.kafka;

import com.motocart.inventory_microservice.kafka.producer.AuditEventProducer;
import com.motocart.inventory_microservice.kafka.producer.NotificationEventProducer;
import com.motocart.library.common.event.AuditEvent;
import com.motocart.library.common.event.InventoryEvent;
import com.motocart.library.common.event.NotificationEvent;
import com.motocart.library.common.types.InventoryActionType;
import com.motocart.library.common.types.NotificationType;
import com.motocart.library.security.Principal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class InventoryEventPublisher {

    private static final String SERVICE_NAME = "inventory-microservice";

    @Value("${moto-cart.kafka.notification.enabled:false}")
    private boolean notificationEnabled;

    private final AuditEventProducer auditEventProducer;
    private final NotificationEventProducer notificationEventProducer;

    public InventoryEventPublisher(AuditEventProducer auditEventProducer, NotificationEventProducer notificationEventProducer) {
        this.auditEventProducer = auditEventProducer;
        this.notificationEventProducer = notificationEventProducer;
    }

    @Async("inventoryExecutor")
    public void publishStockAdded(int productId, int warehouseId, int quantity) {
        auditEventProducer.sendAuditEvent(AuditEvent.builder()
                .auditLogId(UUID.randomUUID().toString())
                .entityId(productId)
                .entityType("STOCK")
                .action("STOCK_ADDED")
                .changedFields(Map.of("warehouseId", warehouseId, "quantity", quantity))
                .userId(getAuthUserId())
                .sourceService(SERVICE_NAME)
                .timeStamp(Instant.now())
                .build());
    }

    @Async("inventoryExecutor")
    public void publishStockUpdated(int productId, int warehouseId, int quantity) {
        auditEventProducer.sendAuditEvent(AuditEvent.builder()
                .auditLogId(UUID.randomUUID().toString())
                .entityId(productId)
                .entityType("STOCK")
                .action("STOCK_UPDATED")
                .changedFields(Map.of("warehouseId", warehouseId, "quantity", quantity))
                .userId(getAuthUserId())
                .sourceService(SERVICE_NAME)
                .timeStamp(Instant.now())
                .build());
    }

    @Async("inventoryExecutor")
    public void publishInventoryAction(InventoryEvent event, InventoryActionType actionType) {
        auditEventProducer.sendAuditEvent(AuditEvent.builder()
                .auditLogId(UUID.randomUUID().toString())
                .entityId(event.getOrderId())
                .entityType("ORDER")
                .action(actionType.name())
                .changedFields(Map.of("productQuantityPairs", event.getProductQuantityPairs()))
                .sourceService(SERVICE_NAME)
                .timeStamp(Instant.now())
                .build());

        if (notificationEnabled && actionType == InventoryActionType.DEDUCT) {
            notificationEventProducer.sendNotificationEvent(NotificationEvent.builder()
                    .notificationType(NotificationType.ORDER_COMPLETE)
                    .subject("Your order has been confirmed")
                    .payload(Map.of("orderId", event.getOrderId()))
                    .build());
        }
    }

    @Async("inventoryExecutor")
    public void publishLowStockAlert(int productId, int availableQuantity, String recipientEmail) {
        if (!notificationEnabled) return;
        notificationEventProducer.sendNotificationEvent(NotificationEvent.builder()
                .notificationType(NotificationType.INVENTORY_ALERT)
                .recipientEmail(recipientEmail)
                .subject("Low stock alert for productId: " + productId)
                .payload(Map.of("productId", productId, "availableQuantity", availableQuantity))
                .build());
    }

    private int getAuthUserId() {
        try {
            return ((Principal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).userId();
        } catch (Exception e) {
            return 0;
        }
    }
}
