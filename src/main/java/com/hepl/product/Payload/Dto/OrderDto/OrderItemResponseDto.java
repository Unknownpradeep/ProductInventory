package com.hepl.product.Payload.Dto.OrderDto;

import lombok.Data;

@Data
public class OrderItemResponseDto {
    private Long productId;
    private String productName;
    private double price;
    private int quantity;
    private double totalPrice;
    private String category;
}
