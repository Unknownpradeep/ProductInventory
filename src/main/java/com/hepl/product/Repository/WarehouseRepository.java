package com.hepl.product.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.hepl.product.model.Warehouse;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
}
