package com.hepl.product.Service;

import org.springframework.data.domain.Page;

import com.hepl.product.Payload.Dto.InvoiceDto.InvoiceResponseDto;

public interface InvoiceService {
    InvoiceResponseDto generateInvoice(String orderCode);
    InvoiceResponseDto get(Long id);
    InvoiceResponseDto getByOrderCode(String orderCode);
    InvoiceResponseDto updateStatus(Long id, String status);
    Page<InvoiceResponseDto> listAll(String search, String status, Long customerId, int page, int size, String sortBy, String sortDir);
    void delete(Long id);
}
