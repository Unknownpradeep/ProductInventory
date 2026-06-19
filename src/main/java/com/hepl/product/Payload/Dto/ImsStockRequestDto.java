package com.hepl.product.Payload.Dto;

import java.util.List;
import lombok.Data;

@Data
public class ImsStockRequestDto {
    private String orderCode;
    private String outletCode;
    private String outletName;
    private String requestedDate;
    private String notes;
    private List<ImsItemDto> items;

    @Data
    public static class ImsItemDto {
        private String productCode;
        private String productName;
        private int quantityRequested;
    }
}
