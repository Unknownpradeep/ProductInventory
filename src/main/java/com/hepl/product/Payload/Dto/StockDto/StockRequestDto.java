package com.hepl.product.Payload.Dto.StockDto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockRequestDto {
    @NotNull(message = "Product ID is required")
    private Long productId;
    @Min(value = 0, message = "Quantity cannot be negative")
    private int quantity;
    @NotBlank(message = "Stock type is required")
    private String type;



}
