package com.hepl.product.Payload.Dto.ProductDto;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;

@Data
public class ProductRequestDto {
    @NotBlank(message = "Product name is required")
    private String name;

    @NotNull(message = "CategoryId is required")
    private Long categoryId;

    @Positive(message = "Price must be greater than 0")
    private double price;

    @Min(value = 0, message = "Quantity cannot be negative")
    private int quantity;

    @NotNull(message = "CustomerId is required")
    private Long customerId;
}
