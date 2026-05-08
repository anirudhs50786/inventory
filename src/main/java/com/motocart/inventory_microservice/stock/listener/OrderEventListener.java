package com.motocart.inventory_microservice.stock.listener;

import com.motocart.inventory_microservice.stock.service.StockManagementService;
import com.motocart.library.common.event.InventoryEvent;
import com.motocart.library.kafka.KafkaTopics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderEventListener {

    private final StockManagementService stockManagementService;

    public OrderEventListener(StockManagementService stockManagementService) {
        this.stockManagementService = stockManagementService;
    }

    @KafkaListener(topics = KafkaTopics.INVENTORY_EVENTS, groupId = "inventory-group")
    public void listen(InventoryEvent event) {
        log.debug("Received inventory event: {} for orderId: {}", event.getActionType(), event.getOrderId());
        switch (event.getActionType()) {
            case RESERVE -> stockManagementService.reserveStock(event);
            case RELEASE -> stockManagementService.releaseStock(event);
            case DEDUCT -> stockManagementService.deductStock(event);
        }
    }
}
