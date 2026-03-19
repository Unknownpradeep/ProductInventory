package com.hepl.product.Payload.Dto.OrderDto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderRequestDto {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    @NotNull(message = "Order items are required")
    @Valid
    private List<OrderItemDto> orderItems;
}
