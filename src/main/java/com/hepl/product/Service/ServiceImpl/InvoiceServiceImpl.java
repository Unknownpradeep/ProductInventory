package com.hepl.product.Service.ServiceImpl;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.hepl.product.Payload.Dto.InvoiceDto.InvoiceResponseDto;
import com.hepl.product.Repository.InvoiceRepository;
import com.hepl.product.Repository.OrderRepository;
import com.hepl.product.Service.InvoiceService;
import com.hepl.product.model.Invoice;
import com.hepl.product.model.Order;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;

    @Override
    public InvoiceResponseDto generateInvoice(String orderCode) {

        // Check if invoice already exists for this order
        if (invoiceRepository.existsByOrderCode(orderCode)) {
            // Return existing invoice (must be unique per order)
            return mapToDto(invoiceRepository.findByOrderCode(orderCode)
                    .orElseThrow(() -> new RuntimeException("Invoice not found")));
        }

        // Find the order
        Order order = orderRepository.findByOrderCode(orderCode);
        if (order == null) {
            throw new RuntimeException("Order not found: " + orderCode);
        }

        // Only generate invoice if order status is CONFIRMED or DELIVERED
        if (!"CONFIRMED".equalsIgnoreCase(order.getStatus()) &&
            !"DELIVERED".equalsIgnoreCase(order.getStatus())) {
            throw new RuntimeException("Invoice can only be generated for CONFIRMED or DELIVERED orders");
        }

        // Create invoice
        Invoice invoice = new Invoice();
        invoice.setInvoiceCode("INV-" + orderCode);
        invoice.setOrder(order);
        invoice.setOrderCode(orderCode);
        invoice.setCustomer(order.getCustomer());
        invoice.setCustomerName(order.getCustomerName());
        invoice.setCustomerEmail(order.getCustomerEmail());
        invoice.setCustomerAddress(order.getCustomer() != null ? order.getCustomer().getAddress() : null);
        invoice.setBaseTotal(order.getBaseTotal() != null ? order.getBaseTotal() : 0.0);
        invoice.setTotalDiscount(order.getTotalDiscount() != null ? order.getTotalDiscount() : 0.0);
        invoice.setTotalTax(order.getTotalTax() != null ? order.getTotalTax() : 0.0);
        invoice.setFinalAmount(order.getTotalPrice());
        invoice.setStatus("PENDING");
        invoice.setInvoiceDate(LocalDateTime.now());
        invoice.setUpdatedAt(LocalDateTime.now());

        return mapToDto(invoiceRepository.save(invoice));
    }

    @Override
    public InvoiceResponseDto get(Long id) {
        return mapToDto(invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice Not Found")));
    }

    @Override
    public InvoiceResponseDto getByOrderCode(String orderCode) {
        return mapToDto(invoiceRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Invoice not found for order: " + orderCode)));
    }

    @Override
    public InvoiceResponseDto updateStatus(Long id, String status) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice Not Found"));

        // If status is APPROVED, check for duplicate
        if ("APPROVED".equalsIgnoreCase(status)) {
            if ("APPROVED".equalsIgnoreCase(invoice.getStatus())) {
                throw new RuntimeException("Invoice is already APPROVED");
            }
        }

        invoice.setStatus(status.toUpperCase());
        invoice.setUpdatedAt(LocalDateTime.now());
        return mapToDto(invoiceRepository.save(invoice));
    }

    @Override
    public Page<InvoiceResponseDto> listAll(String search, String status, Long customerId, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        return invoiceRepository.searchAndFilter(search, status, customerId, PageRequest.of(page, size, sort))
                .map(this::mapToDto);
    }

    @Override
    public void delete(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice Not Found"));
        invoice.setDeleted(true);
        invoiceRepository.save(invoice);
    }

    private InvoiceResponseDto mapToDto(Invoice invoice) {
        InvoiceResponseDto dto = new InvoiceResponseDto();
        dto.setId(invoice.getId());
        dto.setInvoiceCode(invoice.getInvoiceCode());
        dto.setOrderCode(invoice.getOrderCode());
        dto.setCustomerName(invoice.getCustomerName());
        dto.setCustomerEmail(invoice.getCustomerEmail());
        dto.setCustomerAddress(invoice.getCustomerAddress());
        dto.setBaseTotal(invoice.getBaseTotal());
        dto.setTotalDiscount(invoice.getTotalDiscount());
        dto.setTotalTax(invoice.getTotalTax());
        dto.setFinalAmount(invoice.getFinalAmount());
        dto.setStatus(invoice.getStatus());
        dto.setInvoiceDate(invoice.getInvoiceDate());
        return dto;
    }
}
