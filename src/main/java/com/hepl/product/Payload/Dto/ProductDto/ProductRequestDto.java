package com.hepl.product.Payload.Dto.ProductDto;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;

@Data
public class ProductRequestDto {
    @NotBlank(message = "Product name is required")
    private String name;

   

    @Positive(message = "Price must be greater than 0")
    private double price;

    @Min(value = 0, message = "Quantity cannot be negative")
    private int quantity;
    
     private LocalDate ExpiryDate;
     private int saleableStock;
     private int nonSaleableStock;
     private String sku;
     private String uom;
    
    private Long divisionId;

}
