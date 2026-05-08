package com.motocart.inventory_microservice.warehouse.service;

import com.motocart.library.common.dto.WarehouseDTO;
import com.motocart.inventory_microservice.warehouse.entity.WarehouseEntity;
import com.motocart.inventory_microservice.warehouse.repository.WarehouseRepository;
import com.motocart.library.common.types.Permission;
import com.motocart.library.security.authentication.EntitlementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class WarehouseManagement {

    private final WarehouseRepository warehouseRepository;
    private final EntitlementService entitlementService;

    public WarehouseManagement(WarehouseRepository warehouseRepository, EntitlementService entitlementService) {
        this.warehouseRepository = warehouseRepository;
        this.entitlementService = entitlementService;
    }

    public List<WarehouseDTO> getAllWarehouses() {
        entitlementService.canAccess(Permission.WAREHOUSE_VIEW);
        return warehouseRepository.findAll().stream()
                .map(w -> WarehouseDTO.builder()
                        .warehouseId(w.getWarehouseId())
                        .warehouseName(w.getWarehouseName())
                        .location(w.getLocation())
                        .isActive(w.isActive())
                        .build())
                .toList();
    }

    public void addWarehouse(WarehouseDTO warehouseDTO) {
        entitlementService.canAccess(Permission.WAREHOUSE_CREATE);
        WarehouseEntity warehouse = WarehouseEntity.builder()
                .warehouseName(warehouseDTO.getWarehouseName())
                .location(warehouseDTO.getLocation())
                .isActive(true)
                .build();
        warehouseRepository.save(warehouse);
        log.debug("Saved new warehouse: {}", warehouseDTO.getWarehouseName());
    }

    public void updateWarehouse(WarehouseDTO warehouseDTO) {
        entitlementService.canAccess(Permission.WAREHOUSE_UPDATE);
        WarehouseEntity warehouse = warehouseRepository.findById(warehouseDTO.getWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + warehouseDTO.getWarehouseId()));
        warehouse.setWarehouseName(warehouseDTO.getWarehouseName());
        warehouse.setLocation(warehouseDTO.getLocation());
        warehouse.setActive(warehouseDTO.isActive());
        warehouseRepository.save(warehouse);
        log.debug("Updated warehouse: {}", warehouseDTO.getWarehouseId());
    }

    public void deactivateWarehouse(int warehouseId) {
        entitlementService.canAccess(Permission.WAREHOUSE_DELETE);
        WarehouseEntity warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + warehouseId));
        warehouse.setActive(false);
        warehouseRepository.save(warehouse);
        log.debug("Deactivated warehouse: {}", warehouseId);
    }
}
