package com.hepl.product.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hepl.product.Payload.Dto.StockDto.StockRequestDto;
import com.hepl.product.Payload.Response.ApiResponse;
import com.hepl.product.Service.StockService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockController {

    @Autowired
    private StockService service;

    @PostMapping
    public ResponseEntity<ApiResponse> addStock(@RequestBody StockRequestDto stock) {
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.CREATED.value(), "Stock added successfully", service.addStock(stock)));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse> getAvailableStock(@RequestParam Long productId) {
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(), "Success", service.getAvailableStock(productId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getAllStocks(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long productId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(), "Success",
                service.listAll(search, type, productId, page, size, sortBy, sortDir)));
    }
}
