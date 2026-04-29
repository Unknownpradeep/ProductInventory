package com.hepl.product.Payload.Dto.StockDto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class StockRespnseDto {
    private Long id;
    private Long productId;
    private String productName;
    private int quantity;
    private LocalDateTime createdAt= LocalDateTime.now();
    private String type;

}
