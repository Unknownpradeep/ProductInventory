package com.hepl.product.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hepl.product.Payload.Dto.OrderDto.OrderRequestDto;
import com.hepl.product.Payload.Response.ApiResponse;
import com.hepl.product.Service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    @Autowired
    private OrderService service;

    @GetMapping
    public ResponseEntity<ApiResponse> getAllOrders() {
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(), "Success", service.listAll())
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(), "Success", service.get(id))
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createOrder(@Valid @RequestBody OrderRequestDto dto) {
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.CREATED.value(), "Order Created", service.save(dto))
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateOrder(@PathVariable Long id, @Valid @RequestBody OrderRequestDto dto) {
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(), "Order Updated", service.update(id, dto))
        );
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(), "Order Status Updated", service.updateStatus(id, status))
        );
    }

    @PutMapping("/{id}/payment")
    public ResponseEntity<ApiResponse> updatePaymentStatus(@PathVariable Long id, @RequestParam String paymentStatus) {
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(), "Payment Status Updated", service.updatePaymentStatus(id, paymentStatus))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteOrder(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(), "Order Deleted", null)
        );
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse> getOrdersByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(), "Success", service.findByCustomer(customerId))
        );
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse> getOrdersByStatus(@PathVariable String status) {
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(), "Success", service.findByStatus(status))
        );
    }
}
