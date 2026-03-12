package com.hepl.product.model;


import com.fasterxml.jackson.annotation.JsonIgnore;

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
     private String name;
     
     @ManyToOne
     @JoinColumn(name = "category_id")
     private Category categoryObj;
     
     @Column(name = "category")
     private String category;
     
     private double price;
     private int quantity;
     
     @Column(unique=true,nullable=false)
     private String code;
     
     @ManyToOne
     @JoinColumn(name="customer_id")
     private Customer customerObj;
     
     @Column(name = "customer")
     private String customer;


}
