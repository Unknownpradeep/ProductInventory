package com.hepl.product.Payload.Dto.OrderDto;

import java.time.LocalDateTime;
import java.util.List;

import com.hepl.product.Payload.Dto.CustomerDto.CustomerResponseDto;

import lombok.Data;

@Data
public class OrderResponseDto {
    private Long id;
    private String orderCode;
    private LocalDateTime orderDate;
    private CustomerResponseDto customer;
    private List<OrderItemResponseDto> products;
    private String paymentStatus;
    private String shippingAddress;
    private String status;
    private double totalAmount;
}
