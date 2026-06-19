package com.hepl.product.model;

import java.time.LocalDate;

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


     @Column(unique=true,nullable=false)
     private String code;

     private String name;
     private double price;
     private int quantity;
     
     private String sku;
     private String uom;
     private String batchcode;
     private boolean deleted=false;
     private double gstpercentage;
     private double discount;

     @Column(name = "expiry_date")
     @com.fasterxml.jackson.annotation.JsonProperty("ExpiryDate")
     private LocalDate ExpiryDate;


    
     @ManyToOne
     @JoinColumn(name = "division_id")
     private Division division;

    
     @Column(name = "division_name")
     private String divisionName;

    
}
