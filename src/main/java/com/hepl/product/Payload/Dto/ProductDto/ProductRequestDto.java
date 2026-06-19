package com.hepl.product.Payload.Dto.ProductDto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;

@Data
public class ProductRequestDto {
    @NotBlank(message = "Product name is required")
    private String name;

    @Positive(message = "Price must be greater than 0")
    private double price;

    @Min(value = 0, message = "Quantity cannot be negative")
    private int quantity;
   
    private String sku;
    private String uom;
    private String batchcode;
    private double gstpercentage;
    private double discount;

    @com.fasterxml.jackson.annotation.JsonProperty("ExpiryDate")
    private java.time.LocalDate ExpiryDate;

    @NotBlank(message = "Division name is required")
    private String divisionName;
}
