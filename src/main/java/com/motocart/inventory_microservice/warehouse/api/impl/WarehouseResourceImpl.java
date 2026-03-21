package com.motocart.inventory_microservice.warehouse.api.impl;

import com.motocart.inventory_microservice.warehouse.api.WarehouseResource;
import com.motocart.library.common.dto.WarehouseDTO;
import com.motocart.inventory_microservice.warehouse.service.WarehouseManagement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/warehouse")
@Slf4j
public class WarehouseResourceImpl implements WarehouseResource {

    private final WarehouseManagement warehouseManagement;

    public WarehouseResourceImpl(WarehouseManagement warehouseManagement) {
        this.warehouseManagement = warehouseManagement;
    }

    @PostMapping(produces = "application/json")
    @Override
    public ResponseEntity<String> addWarehouse(@RequestBody WarehouseDTO warehouseDTO) {
        try {
            warehouseManagement.addWarehouse(warehouseDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body("Warehouse created successfully");
        } catch (Exception e) {
            log.error("Error creating warehouse: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create warehouse");
        }
    }

    @PutMapping(produces = "application/json")
    @Override
    public ResponseEntity<String> updateWarehouse(@RequestBody WarehouseDTO warehouseDTO) {
        try {
            warehouseManagement.updateWarehouse(warehouseDTO);
            return ResponseEntity.status(HttpStatus.OK).body("Warehouse updated successfully");
        } catch (IllegalArgumentException e) {
            log.error("Warehouse not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating warehouse: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update warehouse");
        }
    }

    @DeleteMapping(produces = "application/json")
    @Override
    public ResponseEntity<String> deleteWarehouse(@RequestParam int warehouseId) {
        try {
            warehouseManagement.deactivateWarehouse(warehouseId);
            return ResponseEntity.status(HttpStatus.OK).body("Warehouse deactivated successfully");
        } catch (IllegalArgumentException e) {
            log.error("Warehouse not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error deleting warehouse: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete warehouse");
        }
    }
}
