package com.motocart.inventory_microservice.kafka;

import com.motocart.inventory_microservice.kafka.producer.AuditEventProducer;
import com.motocart.inventory_microservice.kafka.producer.NotificationEventProducer;
import com.motocart.inventory_microservice.stock.entity.StockEntity;
import com.motocart.library.common.event.AuditEvent;
import com.motocart.library.common.event.InventoryEvent;
import com.motocart.library.common.event.NotificationEvent;
import com.motocart.library.common.types.AuditEntityType;
import com.motocart.library.common.types.InventoryActionType;
import com.motocart.library.common.types.NotificationType;
import com.motocart.library.security.Principal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
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
    public void publishStockAdded(StockEntity stockEntity) {
        Map<String, AuditEvent.FieldChangePair> changedFields = new HashMap<>();
        AuditEvent.addChange(changedFields, "Quantity", null, stockEntity.getQuantity());
        AuditEvent.addChange(changedFields, "Last Updated", null, stockEntity.getLastUpdated());
        AuditEvent.addChange(changedFields, "Warehouse Id", null, stockEntity.getWarehouse().getWarehouseId());
        AuditEvent.addChange(changedFields, "Product Id", null, stockEntity.getProductId());
        AuditEvent.addChange(changedFields, "Reserved Quantity", null, stockEntity.getReservedQuantity());


        auditEventProducer.sendAuditEvent(AuditEvent.builder()
                .auditLogId(UUID.randomUUID().toString())
                .entityId(stockEntity.getProductId())
                .entityType(AuditEntityType.STOCK)
                .action("STOCK_ADDED")
                .changedFieldsPairMap(changedFields)
                .userId(getAuthUserId())
                .sourceService(SERVICE_NAME)
                .timeStamp(Instant.now())
                .build());
    }

    @Async("inventoryExecutor")
    public void publishStockUpdated(StockEntity oldStock, StockEntity newStock) {
        Map<String, AuditEvent.FieldChangePair> changedFields = new HashMap<>();
        AuditEvent.addChange(changedFields, "Stock Id", oldStock.getStockId(), oldStock.getStockId());
        AuditEvent.addChange(changedFields, "Quantity", oldStock.getQuantity(), newStock.getQuantity());
        AuditEvent.addChange(changedFields, "Last Updated", oldStock.getLastUpdated(), newStock.getLastUpdated());
        AuditEvent.addChange(changedFields, "Warehouse Id", oldStock.getWarehouse().getWarehouseId(), newStock.getWarehouse().getWarehouseId());
        AuditEvent.addChange(changedFields, "Product Id", oldStock.getProductId(), newStock.getProductId());
        AuditEvent.addChange(changedFields, "Reserved Quantity", oldStock.getReservedQuantity(), newStock.getReservedQuantity());

        auditEventProducer.sendAuditEvent(AuditEvent.builder()
                .auditLogId(UUID.randomUUID().toString())
                .entityId(oldStock.getProductId())
                .entityType(AuditEntityType.STOCK)
                .action("STOCK_UPDATED")
                .changedFieldsPairMap(changedFields)
                .userId(getAuthUserId())
                .sourceService(SERVICE_NAME)
                .timeStamp(Instant.now())
                .build());
    }

    @Async("inventoryExecutor")
    public void publishInventoryAction(InventoryEvent event, InventoryActionType actionType) {
        Map<String, AuditEvent.FieldChangePair> changedFields = new HashMap<>();
        event.getProductQuantityPairs().forEach(pair ->
                AuditEvent.addChange(changedFields, "Product Id: " + pair.productId(), pair.quantity(), pair.quantity())
        );
        AuditEvent.addChange(changedFields, "Order Id", event.getOrderId(), event.getOrderId());
        AuditEvent.addChange(changedFields, "Action Type", null, actionType.name());

        auditEventProducer.sendAuditEvent(AuditEvent.builder()
                .auditLogId(UUID.randomUUID().toString())
                .entityId(event.getOrderId())
                .entityType(AuditEntityType.ORDER)
                .action(actionType.name())
                .changedFieldsPairMap(changedFields)
                .sourceService(SERVICE_NAME)
                .timeStamp(Instant.now())
                .build());
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
