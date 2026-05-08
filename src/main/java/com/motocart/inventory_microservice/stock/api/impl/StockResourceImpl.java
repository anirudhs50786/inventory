package com.motocart.inventory_microservice.stock.api.impl;

import com.motocart.inventory_microservice.stock.api.StockResource;
import com.motocart.library.common.dto.StockDTO;
import com.motocart.inventory_microservice.stock.service.StockManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stock")
@Slf4j
public class StockResourceImpl implements StockResource {

    private final StockManagementService stockManagementService;

    public StockResourceImpl(StockManagementService stockManagementService) {
        this.stockManagementService = stockManagementService;
    }

    @PostMapping(produces = "application/json")
    @Override
    public ResponseEntity<String> addStock(@RequestBody StockDTO stockDTO) {
        try {
            stockManagementService.addStock(stockDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body("Stock added successfully");
        } catch (IllegalArgumentException e) {
            log.error("Invalid request for addStock: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error adding stock: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to add stock");
        }
    }

    @PutMapping(produces = "application/json")
    @Override
    public ResponseEntity<String> updateStock(@RequestBody StockDTO stockDTO) {
        try {
            stockManagementService.updateStock(stockDTO);
            return ResponseEntity.status(HttpStatus.OK).body("Stock updated successfully");
        } catch (IllegalArgumentException e) {
            log.error("Stock record not found: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating stock: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update stock");
        }
    }

    @GetMapping(path = "/{productId}", produces = "application/json")
    @Override
    public ResponseEntity<StockDTO> checkStock(@PathVariable int productId) {
        try {
            StockDTO stockDTO = stockManagementService.checkStock(productId);
            return ResponseEntity.status(HttpStatus.OK).body(stockDTO);
        } catch (Exception e) {
            log.error("Error checking stock for productId {}: {}", productId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
