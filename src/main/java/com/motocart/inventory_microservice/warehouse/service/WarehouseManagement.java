package com.motocart.inventory_microservice.warehouse.service;

import com.motocart.library.common.dto.WarehouseDTO;
import com.motocart.inventory_microservice.warehouse.entity.WarehouseEntity;
import com.motocart.inventory_microservice.warehouse.repository.WarehouseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WarehouseManagement {

    private final WarehouseRepository warehouseRepository;

    public WarehouseManagement(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    public void addWarehouse(WarehouseDTO warehouseDTO) {
        WarehouseEntity warehouse = WarehouseEntity.builder()
                .warehouseName(warehouseDTO.getWarehouseName())
                .location(warehouseDTO.getLocation())
                .isActive(true)
                .build();
        warehouseRepository.save(warehouse);
        log.debug("Saved new warehouse: {}", warehouseDTO.getWarehouseName());
    }

    public void updateWarehouse(WarehouseDTO warehouseDTO) {
        WarehouseEntity warehouse = warehouseRepository.findById(warehouseDTO.getWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + warehouseDTO.getWarehouseId()));
        warehouse.setWarehouseName(warehouseDTO.getWarehouseName());
        warehouse.setLocation(warehouseDTO.getLocation());
        warehouse.setActive(warehouseDTO.isActive());
        warehouseRepository.save(warehouse);
        log.debug("Updated warehouse: {}", warehouseDTO.getWarehouseId());
    }

    public void deactivateWarehouse(int warehouseId) {
        WarehouseEntity warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + warehouseId));
        warehouse.setActive(false);
        warehouseRepository.save(warehouse);
        log.debug("Deactivated warehouse: {}", warehouseId);
    }
}
