package com.hepl.product.Controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hepl.product.Payload.Dto.ImsStockRequestDto;
import com.hepl.product.Payload.Response.ApiResponse;
import com.hepl.product.Repository.ProductRepository;
import com.hepl.product.Repository.StockPurchaseRequestRepository;
import com.hepl.product.model.Product;
import com.hepl.product.model.StockPurchaseRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/stock-requests")
@RequiredArgsConstructor
public class ExternalStockRequestController {

    private final StockPurchaseRequestRepository requestRepo;
    private final ProductRepository productRepo;

    @PostMapping
    public ResponseEntity<ApiResponse> receiveExternalStockRequest(@RequestBody ImsStockRequestDto dto) {
        log.info("[External IMS] Received stock request push for orderCode={}, outletCode={}", 
                 dto.getOrderCode(), dto.getOutletCode());
        
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            return ResponseEntity.badRequest().body(
                new ApiResponse(HttpStatus.BAD_REQUEST.value(), "Items list cannot be empty", null)
            );
        }

        List<StockPurchaseRequest> savedRequests = new ArrayList<>();

        try {
            // First pass: validate all products exist
            for (ImsStockRequestDto.ImsItemDto item : dto.getItems()) {
                Product product = productRepo.findByCode(item.getProductCode())
                    .orElseGet(() -> productRepo.findBySku(item.getProductCode())
                    .orElseGet(() -> productRepo.findByNameIgnoreCase(item.getProductName()).orElse(null)));

                if (product == null) {
                    log.warn("[External IMS] Product not found in IMS for code={} or name={}", 
                             item.getProductCode(), item.getProductName());
                    return ResponseEntity.badRequest().body(
                        new ApiResponse(HttpStatus.BAD_REQUEST.value(), 
                                        "Product not found for code: " + item.getProductCode() + " / name: " + item.getProductName(), null)
                    );
                }

                if (product.getExpiryDate() != null && product.getExpiryDate().isBefore(java.time.LocalDate.now())) {
                    log.warn("[External IMS] Product is expired for code={} or name={}", 
                             item.getProductCode(), item.getProductName());
                    return ResponseEntity.badRequest().body(
                        new ApiResponse(HttpStatus.BAD_REQUEST.value(), 
                                        "Product is expired: " + product.getName() + " (Expired on " + product.getExpiryDate() + ")", null)
                    );
                }
            }

            // Second pass: save requests
            for (ImsStockRequestDto.ImsItemDto item : dto.getItems()) {
                Product product = productRepo.findByCode(item.getProductCode())
                    .orElseGet(() -> productRepo.findBySku(item.getProductCode())
                    .orElseGet(() -> productRepo.findByNameIgnoreCase(item.getProductName()).orElse(null)));

                StockPurchaseRequest req = new StockPurchaseRequest();
                req.setProduct(product);
                req.setProductName(product.getName());
                req.setQuantity(item.getQuantityRequested());
                
                String reason = dto.getNotes();
                if (reason == null || reason.isBlank()) {
                    reason = "External order: " + dto.getOrderCode();
                } else {
                    reason = reason + " (Order: " + dto.getOrderCode() + ")";
                }
                req.setReason(reason);
                
                req.setRequestedBy("Outlet: " + dto.getOutletName() + " (" + dto.getOutletCode() + ")");
                req.setStatus("PENDING");
                req.setCreatedAt(LocalDateTime.now());

                savedRequests.add(requestRepo.save(req));
            }

            log.info("[External IMS] Successfully created {} stock purchase request(s) for orderCode={}", 
                     savedRequests.size(), dto.getOrderCode());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse(HttpStatus.CREATED.value(), "Stock request(s) received and created", savedRequests)
            );

        } catch (Exception e) {
            log.error("[External IMS] Failed to process stock request for orderCode={}", dto.getOrderCode(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error: " + e.getMessage(), null)
            );
        }
    }
}
