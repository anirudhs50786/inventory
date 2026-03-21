package com.motocart.inventory_microservice.stock.service;

import com.motocart.library.common.dto.StockDTO;
import com.motocart.inventory_microservice.stock.entity.StockEntity;
import com.motocart.inventory_microservice.stock.repository.StockRepository;
import com.motocart.inventory_microservice.warehouse.entity.WarehouseEntity;
import com.motocart.inventory_microservice.warehouse.repository.WarehouseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
public class StockManagementService {

    private final StockRepository stockRepository;
    private final WarehouseRepository warehouseRepository;

    public StockManagementService(StockRepository stockRepository, WarehouseRepository warehouseRepository) {
        this.stockRepository = stockRepository;
        this.warehouseRepository = warehouseRepository;
    }

    public void addStock(StockDTO stockDTO) {
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
    }

    public void updateStock(StockDTO stockDTO) {
        StockEntity stock = stockRepository.findByProductIdAndWarehouse_WarehouseId(stockDTO.getProductId(), stockDTO.getWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Stock record not found for productId: " + stockDTO.getProductId() + " in warehouseId: " + stockDTO.getWarehouseId()));
        stock.setQuantity(stockDTO.getQuantity());
        stock.setLastUpdated(Instant.now());
        stockRepository.save(stock);
        log.debug("Updated stock for productId: {} in warehouseId: {}", stockDTO.getProductId(), stockDTO.getWarehouseId());
    }

    public StockDTO checkStock(int productId) {
        int totalAvailable = stockRepository.getTotalAvailableQuantityByProductId(productId);
        return StockDTO.builder()
                .productId(productId)
                .availableQuantity(totalAvailable)
                .build();
    }
}
