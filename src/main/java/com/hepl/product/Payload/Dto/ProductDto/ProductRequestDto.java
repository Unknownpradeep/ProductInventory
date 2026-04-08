package com.hepl.product.Payload.Dto.ProductDto;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Min;

@Data
public class ProductRequestDto {
    @NotBlank(message = "Product name is required")
    private String name;

   

    @Positive(message = "Price must be greater than 0")
    private double price;

    @Min(value = 0, message = "Quantity cannot be negative")
    private int quantity;
   
    //  @JsonProperty("expiryDate")
    //  @JsonAlias({"ExpiryDate"})

     
     private LocalDate expiryDate;
     @JsonProperty("saleableStock")
     private int saleableStock;
    @JsonProperty("nonSaleableStock")
     private int nonSaleableStock;
     private String sku;
     private String uom;
   @NotBlank(message = "Division name is required")
    private String divisionName;

}
