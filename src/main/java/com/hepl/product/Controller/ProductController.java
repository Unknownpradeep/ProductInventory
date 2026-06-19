package com.hepl.product.Controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.hepl.product.Payload.Dto.ProductDto.ProductRequestDto;
import com.hepl.product.Payload.Dto.ProductDto.ProductResponseDto;
import com.hepl.product.Payload.Response.ApiResponse;
import com.hepl.product.Service.ProductImportExportService;
import com.hepl.product.Service.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/product")
@RequiredArgsConstructor
public class ProductController {

    @Autowired
    private ProductService service;

    @Autowired
    private ProductImportExportService importExportService;

    // ─── Standard CRUD ────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse> getAllProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Long divisionId,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Success",
                        service.listAll(search, code, divisionId, minPrice, maxPrice, page, size, sortBy, sortDir)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Success", service.get(id)));
    }

    @PostMapping
    public ResponseEntity<?> addProduct(@Valid @RequestBody ProductRequestDto dto) {
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Success", service.save(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateProduct(@Valid @PathVariable Long id,
            @RequestBody ProductRequestDto dto) {
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Success", service.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteProduct(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Product deleted successfully", null));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse> getByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Success", service.findByCategory(categoryId)));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse> getByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Success", service.findByCustomer(customerId)));
    }

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse> addProducts(@Valid @RequestBody List<ProductRequestDto> dtos) {
        return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Success", service.saveAll(dtos)));
    }

    // ─── Attachments ──────────────────────────────────────────────────────────

    @PostMapping(value = "/{id}/attachments", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse> addAttachment(@PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws java.io.IOException {
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Success", service.addAttachment(id, file)));
    }

    @GetMapping("/{id}/attachments")
    public ResponseEntity<ApiResponse> getAttachments(@PathVariable Long id) {
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Success", service.getAttachments(id)));
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<ApiResponse> deleteAttachment(@PathVariable Long id,
            @PathVariable Long attachmentId) {
        service.deleteAttachment(id, attachmentId);
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Attachment deleted successfully", null));
    }

    // ─── Bulk Import / Export ─────────────────────────────────────────────────

    /**
     * POST /api/v1/product/import/excel
     * Upload a .xlsx file and import products row-by-row.
     */
    @PostMapping(value = "/import/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> importFromExcel(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty())
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(HttpStatus.BAD_REQUEST.value(), "File is empty", null));

            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".xlsx"))
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(HttpStatus.BAD_REQUEST.value(), "Only .xlsx files are supported", null));

            Map<String, Object> result = importExportService.importFromExcel(file);
            return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), "Import completed", result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Import failed: " + e.getMessage(), null));
        }
    }

    /**
     * GET /api/v1/product/export/excel
     * Export products as a styled .xlsx file.
     */
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportToExcel(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long divisionId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5000") int size) {
        try {
            Page<ProductResponseDto> productPage = service.listAll(search, null, divisionId, null, null,
                    page, size, "id", "asc");
            byte[] bytes = importExportService.exportToExcel(productPage.getContent());
            String filename = "products_export_" + LocalDate.now() + ".xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/v1/product/export/pdf
     * Export products as a styled PDF report.
     */
    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportToPdf(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long divisionId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5000") int size) {
        try {
            Page<ProductResponseDto> productPage = service.listAll(search, null, divisionId, null, null,
                    page, size, "id", "asc");
            byte[] bytes = importExportService.exportToPdf(productPage.getContent());
            String filename = "products_report_" + LocalDate.now() + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/v1/product/import/template
     * Download a blank Excel import template with headers and a sample row.
     */
    @GetMapping("/import/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        try {
            byte[] bytes = importExportService.generateExcelTemplate();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"product_import_template.xlsx\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
