package com.hepl.product.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hepl.product.Payload.Response.ApiResponse;
import com.hepl.product.Service.InvoiceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    @Autowired
    private InvoiceService service;

    @Autowired
    private com.hepl.product.Repository.InvoiceRepository invoiceRepository;

    // Get all invoices with search and filter
    @GetMapping
    public ResponseEntity<ApiResponse> getAllInvoices(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long customerId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Success",
                        service.listAll(search, status, customerId, page, size, sortBy, sortDir)));
    }

    // Get invoice by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Success", service.get(id)));
    }

    // Generate invoice from orderCode
    @PostMapping("/generate/{orderCode}")
    public ResponseEntity<ApiResponse> generateInvoice(@PathVariable String orderCode) {
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.CREATED.value(), "Invoice Generated", service.generateInvoice(orderCode)));
    }

    // Get invoice by orderCode
    @GetMapping("/order/{orderCode}")
    public ResponseEntity<ApiResponse> getByOrderCode(@PathVariable String orderCode) {
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Success", service.getByOrderCode(orderCode)));
    }

    // Update invoice status
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse> updateStatus(@PathVariable Long id, @RequestParam String status) {
        List<String> validStatus = List.of("PENDING", "APPROVED", "CANCELLED");
        if (!validStatus.contains(status.toUpperCase())) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(HttpStatus.BAD_REQUEST.value(),
                            "Invalid status. Allowed: PENDING, APPROVED, CANCELLED", null));
        }
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Invoice Status Updated", service.updateStatus(id, status)));
    }

    // Delete invoice
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteInvoice(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Invoice Deleted", null));
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportToExcel(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String ids) {
        try {
            java.util.List<com.hepl.product.model.Invoice> data;
            if (ids != null && !ids.isBlank()) {
                String[] idArr = ids.split(",");
                java.util.List<Long> idList = java.util.Arrays.stream(idArr)
                        .map(Long::parseLong)
                        .collect(java.util.stream.Collectors.toList());
                data = invoiceRepository.findAllById(idList);
            } else {
                org.springframework.data.domain.Page<com.hepl.product.model.Invoice> paged = invoiceRepository.searchAndFilter(
                        search, status, null, org.springframework.data.domain.PageRequest.of(0, 5000));
                data = paged.getContent();
            }
            byte[] bytes = service.exportToExcel(data);
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"invoices_" + java.time.LocalDate.now() + ".xlsx\"")
                    .contentType(org.springframework.http.MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (java.io.IOException e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportToPdf(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String ids,
            @RequestParam(required = false, defaultValue = "CLASSIC") String template) {
        try {
            java.util.List<com.hepl.product.model.Invoice> data;
            if (ids != null && !ids.isBlank()) {
                String[] idArr = ids.split(",");
                java.util.List<Long> idList = java.util.Arrays.stream(idArr)
                        .map(Long::parseLong)
                        .collect(java.util.stream.Collectors.toList());
                data = invoiceRepository.findAllById(idList);
            } else {
                org.springframework.data.domain.Page<com.hepl.product.model.Invoice> paged = invoiceRepository.searchAndFilter(
                        search, status, null, org.springframework.data.domain.PageRequest.of(0, 5000));
                data = paged.getContent();
            }
            byte[] bytes = service.exportToPdf(data, template);
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"invoices_" + java.time.LocalDate.now() + ".pdf\"")
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .body(bytes);
        } catch (java.io.IOException e) {
            return ResponseEntity.status(500).build();
        }
    }

    // Download invoice PDF by orderCode
    @GetMapping("/download/order/{orderCode}")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable String orderCode) {
        try {
            com.hepl.product.model.Invoice invoice = invoiceRepository.findByOrderCode(orderCode)
                    .orElse(null);
            if (invoice == null) {
                com.hepl.product.Payload.Dto.InvoiceDto.InvoiceResponseDto dto = service.generateInvoice(orderCode);
                invoice = invoiceRepository.findById(dto.getId()).orElse(null);
            }
            if (invoice == null) {
                return ResponseEntity.notFound().build();
            }
            byte[] bytes = service.exportToPdf(java.util.List.of(invoice), "CLASSIC");
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"Invoice_" + orderCode + ".pdf\"")
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .body(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
