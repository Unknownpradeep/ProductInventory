package com.hepl.product.Payload.Dto.OrderDto;

import lombok.Data;

@Data
public class OrderItemResponseDto {
    private Long productId;
    private String productName;
    private String productCode;
    private String divisionName;
    private int quantity;
    private double price;
    private double discount;
    private double gstpercentage;
    private double taxamount;
    private double totalPrice;
    private String status;
}
