package com.hepl.product.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Data
@Entity(name = "warehouse_stock")
public class WarehouseStock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "available_qty", nullable = false)
    private int availableQty = 0;

    @Column(name = "reserved_qty", nullable = false)
    private int reservedQty = 0;

    public int getQuantity() {
        return this.availableQty;
    }

    public Long getWarehouseId() {
        return this.warehouse != null ? this.warehouse.getId() : null;
    }

    public Long getProductId() {
        return this.product != null ? this.product.getId() : null;
    }
}
