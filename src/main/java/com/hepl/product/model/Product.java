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
@Entity(name = "product")
public class Product{
     @Id
     @GeneratedValue(strategy = GenerationType.IDENTITY)
     private Long id;

     @Column(name = "category")
     private String category;

     @Column(unique=true,nullable=false)
     private String code;

     private String name;
     private double price;
     private int quantity;

     @Column(name = "customer")
     private String customer;

     @Column(name = "email")
     private String email;

     @ManyToOne
     @JoinColumn(name = "category_id")
     private Category categoryObj;

     @ManyToOne
     @JoinColumn(name="customer_id")
     private Customer customerObj;
}
