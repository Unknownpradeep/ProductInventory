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
    private double baseTotal;
    private double totalDiscount;
    private double totalTax;
    private double finalAmount;
    private String status;
    private String paymentStatus;
    
    private List<CustomerResponseDto> customer;
    private List<OrderItemResponseDto> products;
   
    
}
