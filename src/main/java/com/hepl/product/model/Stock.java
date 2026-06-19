package com.hepl.product.model;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Data
@Entity
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
    private String productName;
    private int quantity;
    private String type;
    private LocalDate expiryDate;
    private int saleableStock;
    private int nonSaleableStock;
    private LocalDate createdAt;
    private boolean deleted=false;
}
