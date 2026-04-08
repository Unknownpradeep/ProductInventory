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

    // Get all invoices with search and filter
    @GetMapping
    public ResponseEntity<ApiResponse> getAllInvoices(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long customerId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "asc") String sortDir) {
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
}
