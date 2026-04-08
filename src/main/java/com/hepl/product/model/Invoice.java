package com.hepl.product.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String invoiceCode;

    @OneToOne
    @JoinColumn(name = "order_id", unique = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Order order;

    @Column(name = "order_code")
    private String orderCode;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "customer_address")
    private String customerAddress;

    private double baseTotal;
    private double totalDiscount;
    private double totalTax;
    private double finalAmount;

    private String status; // PENDING, APPROVED, CANCELLED

    private boolean deleted = false;

    private LocalDateTime invoiceDate = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}
