package com.motocart.inventory_microservice.stock.repository;

import com.motocart.inventory_microservice.stock.entity.StockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<StockEntity, Integer> {

    List<StockEntity> findAllByProductId(int productId);

    List<StockEntity> findAllByProductIdIn(List<Integer> productIds);

    Optional<StockEntity> findByProductIdAndWarehouse_WarehouseId(int productId, int warehouseId);

    @Query("SELECT COALESCE(SUM(s.quantity - s.reservedQuantity), 0) FROM StockEntity s WHERE s.productId = :productId")
    int getTotalAvailableQuantityByProductId(int productId);
}
