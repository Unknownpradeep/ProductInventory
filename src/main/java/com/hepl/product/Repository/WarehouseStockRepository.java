package com.hepl.product.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.hepl.product.model.WarehouseStock;

@Repository
public interface WarehouseStockRepository extends JpaRepository<WarehouseStock, Long> {
    List<WarehouseStock> findByWarehouse_Id(Long warehouseId);
    Optional<WarehouseStock> findByWarehouse_IdAndProduct_Id(Long warehouseId, Long productId);
}
