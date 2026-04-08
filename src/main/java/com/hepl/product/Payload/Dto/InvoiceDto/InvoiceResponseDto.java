package com.hepl.product.Payload.Dto.InvoiceDto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class InvoiceResponseDto {
    private Long id;
    private String invoiceCode;
    private String orderCode;
    private String customerName;
    private String customerEmail;
    private String customerAddress;
    private double baseTotal;
    private double totalDiscount;
    private double totalTax;
    private double finalAmount;
    private String status;
    private LocalDateTime invoiceDate;
}
