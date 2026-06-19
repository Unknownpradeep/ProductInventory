package com.hepl.product.Payload.Dto.ProductDto;

import java.io.Serializable;

import lombok.Data;

@Data
public class ProductResponseDto implements Serializable {
    private Long id;
    private String name;
    private double price;
    private int quantity;
    private String DivisionName;
    private String sku;
    private String uom;
    private String batchcode;
    private double gstpercentage;
    private double discount;

    @com.fasterxml.jackson.annotation.JsonProperty("ExpiryDate")
    private java.time.LocalDate ExpiryDate;
}
