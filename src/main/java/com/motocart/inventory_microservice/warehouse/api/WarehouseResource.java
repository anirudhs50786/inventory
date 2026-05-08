package com.motocart.inventory_microservice.warehouse.api;

import com.motocart.library.common.dto.WarehouseDTO;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface WarehouseResource {

    ResponseEntity<List<WarehouseDTO>> getAllWarehouses();

    ResponseEntity<String> addWarehouse(WarehouseDTO warehouseDTO);

    ResponseEntity<String> updateWarehouse(WarehouseDTO warehouseDTO);

    ResponseEntity<String> deleteWarehouse(int warehouseId);
}
