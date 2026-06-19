package com.hepl.product.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class StockPurchaseRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private String productName;
    private int quantity;
    private String reason;
    private String requestedBy;   // username of the VIEWER
    private String status;        // PENDING, APPROVED, REJECTED
    private String remarks;       // admin/manager remarks on approval/rejection
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;
}
