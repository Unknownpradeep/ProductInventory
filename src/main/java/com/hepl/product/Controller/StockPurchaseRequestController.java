package com.hepl.product.Controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.hepl.product.Payload.Response.ApiResponse;
import com.hepl.product.Repository.ProductRepository;
import com.hepl.product.Repository.StockPurchaseRequestRepository;
import com.hepl.product.Service.StockService;
import com.hepl.product.model.StockPurchaseRequest;
import com.hepl.product.Payload.Dto.StockDto.StockRequestDto;
import com.hepl.product.Util.JwtUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/stock-requests")
@RequiredArgsConstructor
public class StockPurchaseRequestController {

    private final StockPurchaseRequestRepository requestRepo;
    private final ProductRepository productRepo;
    private final StockService stockService;
    private final JwtUtil jwtUtil;

    // VIEWER submits a stock request
    @PostMapping
    public ResponseEntity<ApiResponse> submit(@RequestBody Map<String, Object> body) {
        try {
            Long productId = Long.valueOf(body.get("productId").toString());
            int quantity = Integer.parseInt(body.get("quantity").toString());
            String reason = body.getOrDefault("reason", "").toString();
            String requestedBy = body.getOrDefault("requestedBy", "unknown").toString();

            var product = productRepo.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            StockPurchaseRequest req = new StockPurchaseRequest();
            req.setProduct(product);
            req.setProductName(product.getName());
            req.setQuantity(quantity);
            req.setReason(reason);
            req.setRequestedBy(requestedBy);
            req.setStatus("PENDING");
            req.setCreatedAt(LocalDateTime.now());

            return ResponseEntity.ok(new ApiResponse(HttpStatus.CREATED.value(), "Request submitted", requestRepo.save(req)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        }
    }

    // Get all requests (ADMIN/MANAGER)
    @GetMapping
    public ResponseEntity<ApiResponse> getAll(@RequestParam(required = false) String status) {
        List<StockPurchaseRequest> list = status != null && !status.isBlank()
                ? requestRepo.findByStatusOrderByCreatedAtDesc(status)
                : requestRepo.findAllByOrderByCreatedAtDesc();
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Success", list));
    }

    // Get requests by username (VIEWER sees own requests)
    @GetMapping("/my")
    public ResponseEntity<ApiResponse> getMy(@RequestParam String username) {
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Success",
                requestRepo.findByRequestedByOrderByCreatedAtDesc(username)));
    }

    // ADMIN/MANAGER approves a request → creates actual stock IN entry
    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse> approve(@PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String role = jwtUtil.extractRole(token);
            if (role == null || (!role.toUpperCase().contains("ADMIN") && !role.toUpperCase().contains("MANAGER") && !role.toUpperCase().contains("STORES"))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(HttpStatus.FORBIDDEN.value(), "Only ADMIN, MANAGER or STORES can manage requests", null));
            }

            StockPurchaseRequest req = requestRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Request not found"));

            req.setStatus("APPROVED");
            req.setRemarks(body != null ? body.getOrDefault("remarks", "").toString() : "");
            req.setUpdatedAt(LocalDateTime.now());
            requestRepo.save(req);

            StockRequestDto dto = new StockRequestDto();
            dto.setProductId(req.getProduct().getId());
            dto.setQuantity(req.getQuantity());
            dto.setType("IN");
            stockService.addStock(dto);

            return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Request approved and stock updated", req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        }
    }

    // ADMIN/MANAGER rejects a request
    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse> reject(@PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String role = jwtUtil.extractRole(token);
            if (role == null || (!role.toUpperCase().contains("ADMIN") && !role.toUpperCase().contains("MANAGER") && !role.toUpperCase().contains("STORES"))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(HttpStatus.FORBIDDEN.value(), "Only ADMIN, MANAGER or STORES can manage requests", null));
            }

            StockPurchaseRequest req = requestRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Request not found"));

            req.setStatus("REJECTED");
            req.setRemarks(body != null ? body.getOrDefault("remarks", "").toString() : "");
            req.setUpdatedAt(LocalDateTime.now());
            requestRepo.save(req);

            return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Request rejected", req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        }
    }

    // Delete a request
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        requestRepo.deleteById(id);
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Deleted", null));
    }
}
