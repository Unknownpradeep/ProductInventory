package com.hepl.product.model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import lombok.Data;

@Data
@Entity
public class Division {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique=true,nullable=false)
    private String batchcode;
    private String name;
    private boolean deleted=false;
    
    
}
