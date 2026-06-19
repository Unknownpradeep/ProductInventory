package com.hepl.product.Controller;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.hepl.product.Payload.Dto.StockDto.StockRequestDto;
import com.hepl.product.Payload.Response.ApiResponse;
import com.hepl.product.Service.StockImportExportService;
import com.hepl.product.Service.StockService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockController {

    @Autowired
    private StockService service;

    @Autowired
    private StockImportExportService importExportService;

    @Autowired
    private com.hepl.product.Repository.StockRepository stockRepository;

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

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteStock(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Stock deleted successfully", null));
    }

    @PostMapping(value = "/import/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> importFromExcel(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) return ResponseEntity.badRequest().body(new ApiResponse(400, "File is empty", null));
            Map<String, Object> result = importExportService.importFromExcel(file);
            return ResponseEntity.ok(new ApiResponse(200, "Import completed", result));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(new ApiResponse(500, "Import failed: " + e.getMessage(), null));
        }
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportToExcel(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5000") int size) {
        try {
            List<com.hepl.product.model.Stock> data;
            if ((search != null && !search.isBlank()) || (type != null && !type.isBlank())) {
                org.springframework.data.domain.Page<com.hepl.product.model.Stock> paged = stockRepository.searchAndFilter(
                        search, type, null, org.springframework.data.domain.PageRequest.of(0, 5000));
                data = paged.getContent();
            } else {
                data = service.findAllEntities();
            }
            byte[] bytes = importExportService.exportToExcel(data);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"stocks_" + LocalDate.now() + ".xlsx\"")
                    .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (IOException e) { return ResponseEntity.status(500).build(); }
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportToPdf(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5000") int size) {
        try {
            List<com.hepl.product.model.Stock> data;
            if ((search != null && !search.isBlank()) || (type != null && !type.isBlank())) {
                org.springframework.data.domain.Page<com.hepl.product.model.Stock> paged = stockRepository.searchAndFilter(
                        search, type, null, org.springframework.data.domain.PageRequest.of(0, 5000));
                data = paged.getContent();
            } else {
                data = service.findAllEntities();
            }
            byte[] bytes = importExportService.exportToPdf(data);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"stocks_" + LocalDate.now() + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(bytes);
        } catch (IOException e) { return ResponseEntity.status(500).build(); }
    }

    @GetMapping("/import/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        try {
            byte[] bytes = importExportService.generateExcelTemplate();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"stock_template.xlsx\"")
                    .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (IOException e) { return ResponseEntity.status(500).build(); }
    }
}
