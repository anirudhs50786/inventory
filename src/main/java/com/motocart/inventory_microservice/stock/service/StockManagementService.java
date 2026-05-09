package com.motocart.inventory_microservice.stock.service;

import com.motocart.inventory_microservice.kafka.InventoryEventPublisher;
import com.motocart.library.common.dto.StockDTO;
import com.motocart.library.common.event.InventoryEvent;
import com.motocart.inventory_microservice.stock.entity.StockEntity;
import com.motocart.inventory_microservice.stock.repository.StockRepository;
import com.motocart.inventory_microservice.warehouse.entity.WarehouseEntity;
import com.motocart.inventory_microservice.warehouse.repository.WarehouseRepository;
import com.motocart.library.common.types.InventoryActionType;
import com.motocart.library.common.types.Permission;
import com.motocart.library.security.authentication.EntitlementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StockManagementService {

    @Value("${moto-cart.inventory.low-stock-threshold:10}")
    private int lowStockThreshold;

    @Value("${moto-cart.inventory.admin-email}")
    private String adminEmail;

    private final StockRepository stockRepository;
    private final WarehouseRepository warehouseRepository;
    private final EntitlementService entitlementService;
    private final InventoryEventPublisher eventPublisher;

    public StockManagementService(StockRepository stockRepository, WarehouseRepository warehouseRepository,
                                  EntitlementService entitlementService, InventoryEventPublisher eventPublisher) {
        this.stockRepository = stockRepository;
        this.warehouseRepository = warehouseRepository;
        this.entitlementService = entitlementService;
        this.eventPublisher = eventPublisher;
    }

    public void addStock(StockDTO stockDTO) {
        entitlementService.canAccess(Permission.STOCK_CREATE);
        WarehouseEntity warehouse = warehouseRepository.findById(stockDTO.getWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + stockDTO.getWarehouseId()));
        StockEntity stock = StockEntity.builder()
                .productId(stockDTO.getProductId())
                .warehouse(warehouse)
                .quantity(stockDTO.getQuantity())
                .reservedQuantity(0)
                .lastUpdated(Instant.now())
                .build();
        stockRepository.save(stock);
        log.debug("Added stock for productId: {} in warehouseId: {}", stockDTO.getProductId(), stockDTO.getWarehouseId());
        eventPublisher.publishStockAdded(stockDTO.getProductId(), stockDTO.getWarehouseId(), stockDTO.getQuantity());
    }

    public void updateStock(StockDTO stockDTO) {
        entitlementService.canAccess(Permission.STOCK_UPDATE);
        StockEntity stock = stockRepository.findByProductIdAndWarehouse_WarehouseId(stockDTO.getProductId(), stockDTO.getWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Stock record not found for productId: " + stockDTO.getProductId() + " in warehouseId: " + stockDTO.getWarehouseId()));
        stock.setQuantity(stockDTO.getQuantity());
        stock.setLastUpdated(Instant.now());
        stockRepository.save(stock);
        log.debug("Updated stock for productId: {} in warehouseId: {}", stockDTO.getProductId(), stockDTO.getWarehouseId());
        eventPublisher.publishStockUpdated(stockDTO.getProductId(), stockDTO.getWarehouseId(), stockDTO.getQuantity());
    }

    public StockDTO checkStock(int productId) {
        entitlementService.canAccess(Permission.STOCK_VIEW);
        int totalAvailable = stockRepository.getTotalAvailableQuantityByProductId(productId);
        return StockDTO.builder()
                .productId(productId)
                .availableQuantity(totalAvailable)
                .build();
    }

    @Transactional
    public void reserveStock(InventoryEvent event) {
        Map<Integer, Integer> requiredQty = toProductQtyMap(event.getProductQuantityPairs());
        Map<Integer, List<StockEntity>> stockByProduct = getStockGroupedByProduct(requiredQty);

        for (var entry : requiredQty.entrySet()) {
            int productId = entry.getKey();
            int needed = entry.getValue();
            List<StockEntity> stocks = stockByProduct.getOrDefault(productId, List.of());

            int available = stocks.stream().mapToInt(s -> s.getQuantity() - s.getReservedQuantity()).sum();
            if (available < needed) {
                throw new IllegalStateException("Insufficient stock for productId: " + productId + ". Available: " + available + ", Required: " + needed);
            }

            int remaining = needed;
            for (StockEntity stock : stocks) {
                if (remaining <= 0) break;
                int canReserve = Math.min(stock.getQuantity() - stock.getReservedQuantity(), remaining);
                stock.setReservedQuantity(stock.getReservedQuantity() + canReserve);
                stock.setLastUpdated(Instant.now());
                remaining -= canReserve;
            }
        }
        stockRepository.saveAll(stockByProduct.values().stream().flatMap(List::stream).toList());
        log.debug("Reserved stock for orderId: {}", event.getOrderId());
        eventPublisher.publishInventoryAction(event, InventoryActionType.RESERVE);
    }

    @Transactional
    public void releaseStock(InventoryEvent event) {
        Map<Integer, Integer> requiredQty = toProductQtyMap(event.getProductQuantityPairs());
        Map<Integer, List<StockEntity>> stockByProduct = getStockGroupedByProduct(requiredQty);

        for (var entry : requiredQty.entrySet()) {
            int productId = entry.getKey();
            int toRelease = entry.getValue();
            List<StockEntity> stocks = stockByProduct.getOrDefault(productId, List.of());

            for (StockEntity stock : stocks) {
                if (toRelease <= 0) break;
                int canRelease = Math.min(stock.getReservedQuantity(), toRelease);
                stock.setReservedQuantity(stock.getReservedQuantity() - canRelease);
                stock.setLastUpdated(Instant.now());
                toRelease -= canRelease;
            }
        }
        stockRepository.saveAll(stockByProduct.values().stream().flatMap(List::stream).toList());
        log.debug("Released stock for orderId: {}", event.getOrderId());
        eventPublisher.publishInventoryAction(event, InventoryActionType.RELEASE);
    }

    @Transactional
    public void deductStock(InventoryEvent event) {
        Map<Integer, Integer> requiredQty = toProductQtyMap(event.getProductQuantityPairs());
        Map<Integer, List<StockEntity>> stockByProduct = getStockGroupedByProduct(requiredQty);

        for (var entry : requiredQty.entrySet()) {
            int productId = entry.getKey();
            int toDeduct = entry.getValue();
            List<StockEntity> stocks = stockByProduct.getOrDefault(productId, List.of());

            for (StockEntity stock : stocks) {
                if (toDeduct <= 0) break;
                int canDeduct = Math.min(stock.getReservedQuantity(), toDeduct);
                stock.setQuantity(stock.getQuantity() - canDeduct);
                stock.setReservedQuantity(stock.getReservedQuantity() - canDeduct);
                stock.setLastUpdated(Instant.now());
                toDeduct -= canDeduct;
            }
        }
        stockRepository.saveAll(stockByProduct.values().stream().flatMap(List::stream).toList());
        log.debug("Deducted stock for orderId: {}", event.getOrderId());
        eventPublisher.publishInventoryAction(event, InventoryActionType.DEDUCT);
        checkAndAlertLowStock(stockByProduct);
    }

    private void checkAndAlertLowStock(Map<Integer, List<StockEntity>> stockByProduct) {
        stockByProduct.forEach((productId, stocks) -> {
            int available = stocks.stream().mapToInt(s -> s.getQuantity() - s.getReservedQuantity()).sum();
            if (available <= lowStockThreshold) {
                log.warn("Low stock alert for productId: {}, available: {}", productId, available);
                eventPublisher.publishLowStockAlert(productId, available, adminEmail);
            }
        });
    }

    private Map<Integer, Integer> toProductQtyMap(List<InventoryEvent.ProductQuantityPair> pairs) {
        return pairs.stream().collect(Collectors.toMap(InventoryEvent.ProductQuantityPair::productId, InventoryEvent.ProductQuantityPair::quantity));
    }

    private Map<Integer, List<StockEntity>> getStockGroupedByProduct(Map<Integer, Integer> requiredQty) {
        return stockRepository.findAllByProductIdIn(List.copyOf(requiredQty.keySet()))
                .stream().collect(Collectors.groupingBy(StockEntity::getProductId));
    }
}
