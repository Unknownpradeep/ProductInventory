package com.hepl.product.Controller;


import java.nio.file.Files;
import java.util.List;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hepl.product.Payload.Dto.OrderDto.OrderRequestDto;
import com.hepl.product.Payload.Response.ApiResponse;
import com.hepl.product.Repository.OrderRepository;
import com.hepl.product.Service.OrderService;
import com.hepl.product.model.Order;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.core.io.Resource;


import org.springframework.http.MediaType;
import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/orders")
//@RequiredArgsConstructor
public class OrderController {

    @Autowired
    private OrderService service;
    
    @Autowired
    private OrderRepository orderRepository;

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
        List<String> validStatus = List.of("PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED");
        if (!validStatus.contains(status.toUpperCase())) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(HttpStatus.BAD_REQUEST.value(), "Invalid status. Allowed: PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED", null));
        }
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(), "Order Status Updated", service.updateStatus(id, status.toUpperCase()))
        );
    }

    @PutMapping("/{id}/payment")
    public ResponseEntity<ApiResponse> updatePaymentStatus(@PathVariable Long id, @RequestParam String paymentStatus) {
        List<String> validPaymentStatus = List.of("PENDING", "SUCCESS");
        if (!validPaymentStatus.contains(paymentStatus.toUpperCase())) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(HttpStatus.BAD_REQUEST.value(), "Invalid payment status. Allowed: PENDING, SUCCESS", null));
        }
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(), "Payment Status Updated", service.updatePaymentStatus(id, paymentStatus.toUpperCase()))
        );
    }

    @DeleteMapping("/id/{id}")
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
    @GetMapping("/qr/{orderCode}")
public ResponseEntity<Resource> getQR(@PathVariable String orderCode) {

   
    Order order = orderRepository.findByOrderCode(orderCode);

    if (order == null) {
        throw new RuntimeException("Order not found");
    }

  
    String filePath = order.getQrCodePath();

    if (filePath == null || filePath.isEmpty()) {
        throw new RuntimeException("QR not generated");
    }

    Path path = Paths.get(filePath);

   
    if (!Files.exists(path)) {
        throw new RuntimeException("QR file not found");
    }

    
    Resource resource;
    try {
        resource = new UrlResource(path.toUri());
    } catch (Exception e) {
        throw new RuntimeException("Error loading QR", e);
    }

   
    return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_PNG)
            .body(resource);
}
   @GetMapping("/code/{orderCode}")
public ResponseEntity<ApiResponse> getOrderByCode(@PathVariable String orderCode) { 
    Order order = orderRepository.findByOrderCode(orderCode);
    if (order == null) {
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.NOT_FOUND.value(), "Order not found", null));
    }
    return ResponseEntity.ok(
        new ApiResponse(HttpStatus.OK.value(), "Success", service.getByCode(orderCode))
    );
}

}

