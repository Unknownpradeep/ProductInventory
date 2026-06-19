package com.hepl.product.Controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.hepl.product.Payload.Dto.OrderDto.OrderRequestDto;
import com.hepl.product.Payload.Response.ApiResponse;
import com.hepl.product.Repository.OrderRepository;
import com.hepl.product.Service.OrderImportExportService;
import com.hepl.product.Service.OrderService;
import com.hepl.product.model.Order;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    @Autowired
    private OrderService service;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderImportExportService importExportService;

    @GetMapping
    public ResponseEntity<ApiResponse> getAllOrders(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime endDate,
            @RequestParam(required = false) String preset,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Success",
                        service.listAll(search, status, paymentStatus, customerId, startDate, endDate, preset, page, size, sortBy, sortDir)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Success", service.get(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createOrder(@Valid @RequestBody OrderRequestDto dto) {
        return ResponseEntity.ok(new ApiResponse(HttpStatus.CREATED.value(), "Order Created", service.save(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateOrder(@PathVariable Long id, @Valid @RequestBody OrderRequestDto dto) {
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Order Updated", service.update(id, dto)));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse> updateStatus(@PathVariable Long id, @RequestParam String status, @RequestParam(required = false) String remark) {
        List<String> validStatus = List.of("CREATED", "PENDING", "CONFIRMED", "STOCK_RESERVED", "READY_FOR_DISPATCH", "PARTIALLY_DISPATCHED", "DISPATCHED", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED");
        String upperStatus = status.toUpperCase(Locale.ROOT);
        if (!validStatus.contains(upperStatus)) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(HttpStatus.BAD_REQUEST.value(),
                            "Invalid status. Allowed: CREATED, PENDING, CONFIRMED, STOCK_RESERVED, READY_FOR_DISPATCH, PARTIALLY_DISPATCHED, DISPATCHED, OUT_FOR_DELIVERY, DELIVERED, CANCELLED", null));
        }
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Order Status Updated", service.updateStatus(id, upperStatus, remark)));
    }

    @PutMapping("/{id}/payment")
    public ResponseEntity<ApiResponse> updatePaymentStatus(@PathVariable Long id, @RequestParam String paymentStatus) {
        List<String> validPaymentStatus = List.of("PENDING", "SUCCESS");
        String upperPayment = paymentStatus.toUpperCase(Locale.ROOT);
        if (!validPaymentStatus.contains(upperPayment)) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(HttpStatus.BAD_REQUEST.value(),
                            "Invalid payment status. Allowed: PENDING, SUCCESS", null));
        }
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Payment Status Updated",
                        service.updatePaymentStatus(id, upperPayment)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteOrder(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Order Deleted", null));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse> getOrdersByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Success", service.findByCustomer(customerId)));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse> getOrdersByStatus(@PathVariable String status) {
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Success", service.findByStatus(status)));
    }

    @GetMapping("/qr/{orderCode}")
    public ResponseEntity<Resource> getQR(@PathVariable String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode);
        if (order == null)
            throw new RuntimeException("Order not found");

        String filePath = order.getQrCodePath();
        if (filePath == null || filePath.isEmpty())
            throw new RuntimeException("QR not generated");

        Path path = Paths.get(filePath).normalize().toAbsolutePath();
        if (!Files.exists(path))
            throw new RuntimeException("QR file not found");

        try {
            Resource resource = new UrlResource(path.toUri());
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(resource);
        } catch (IOException e) {
            throw new RuntimeException("Error loading QR", e);
        }
    }

    @GetMapping("/code/{orderCode}")
    public ResponseEntity<ApiResponse> getOrderByCode(@PathVariable String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode);
        if (order == null) {
            return ResponseEntity.ok(new ApiResponse(HttpStatus.NOT_FOUND.value(), "Order not found", null));
        }
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Success", service.getByCode(orderCode)));
    }

    @PostMapping(value = "/import/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> importFromExcel(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty())
                return ResponseEntity.badRequest().body(new ApiResponse(400, "File is empty", null));
            Map<String, Object> result = importExportService.importFromExcel(file);
            return ResponseEntity.ok(new ApiResponse(200, "Processing complete", result));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(new ApiResponse(500, "Import failed: " + e.getMessage(), null));
        }
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportToExcel(
            @RequestParam(required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5000") int size) {
        try {
            List<Order> data;
            if (search != null && !search.isBlank()) {
                org.springframework.data.domain.Page<Order> paged = orderRepository.searchAndFilter(
                        search, null, null, null, null, null,
                        org.springframework.data.domain.PageRequest.of(0, 5000));
                data = paged.getContent();
            } else {
                data = orderRepository.findAll();
            }
            byte[] bytes = importExportService.exportToExcel(data);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"orders_" + LocalDate.now() + ".xlsx\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportToPdf(
            @RequestParam(required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5000") int size) {
        try {
            List<Order> data;
            if (search != null && !search.isBlank()) {
                org.springframework.data.domain.Page<Order> paged = orderRepository.searchAndFilter(
                        search, null, null, null, null, null,
                        org.springframework.data.domain.PageRequest.of(0, 5000));
                data = paged.getContent();
            } else {
                data = orderRepository.findAll();
            }
            byte[] bytes = importExportService.exportToPdf(data);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"orders_" + LocalDate.now() + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(bytes);
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/import/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        try {
            byte[] bytes = importExportService.generateExcelTemplate();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"order_template.xlsx\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/server-ip")
    public ResponseEntity<ApiResponse> getServerIp() {
        String ip = "localhost";
        try {
            boolean found = false;
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements() && !found) {
                java.net.NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                
                java.util.Enumeration<java.net.InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress addr = addresses.nextElement();
                    if (addr instanceof java.net.Inet4Address) {
                        String hostAddress = addr.getHostAddress();
                        if (hostAddress.startsWith("10.") || hostAddress.startsWith("192.168.") || hostAddress.startsWith("172.")) {
                            ip = hostAddress;
                            found = true;
                            break;
                        }
                    }
                }
            }
            if (ip.equals("localhost")) {
                ip = java.net.InetAddress.getLocalHost().getHostAddress();
            }
        } catch (Exception e) {
            System.err.println("Failed to resolve local IP address: " + e.getMessage());
        }
        return ResponseEntity.ok(new ApiResponse(200, "Success", ip));
    }
}
