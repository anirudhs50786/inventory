package com.motocart.inventory_microservice.warehouse.repository;

import com.motocart.inventory_microservice.warehouse.entity.WarehouseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WarehouseRepository extends JpaRepository<WarehouseEntity, Integer> {
}
