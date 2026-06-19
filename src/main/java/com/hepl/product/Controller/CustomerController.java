package com.hepl.product.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hepl.product.Payload.Dto.CustomerDto.CustomerRequestDto;
  
import com.hepl.product.Payload.Response.ApiResponse;
import com.hepl.product.Service.CustomerService;
import com.hepl.product.model.Customer;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.Map;
import java.time.LocalDate;
import com.hepl.product.Service.CustomerImportExportService;
import com.hepl.product.Payload.Dto.CustomerDto.CustomerResponseDto;
import org.springframework.data.domain.Page;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    @Autowired
    private CustomerService service;

    @Autowired
    private CustomerImportExportService importExportService;

    
    @GetMapping
    public ResponseEntity<ApiResponse> getAllCustomers(
                                                      @RequestParam(value = "search", required = false) String search,
                                                      @RequestParam(required=false)String name,
                                                      @RequestParam(required=false)String email,
                                                      @RequestParam(required=false)String state,
                                                      @RequestParam(required=false)String country,
                                                      
                                                      @RequestParam(value = "page", defaultValue = "0") int page,
                                                      @RequestParam(value = "size", defaultValue = "10") int size,
                                                    @RequestParam(value="sortBy", defaultValue = "id") String sortBy,
                                                    @RequestParam(value="sortDir", defaultValue = "asc") String sortDir){
               return ResponseEntity.ok(
               new ApiResponse(HttpStatus.OK.value(),"Success",service.listAll(search, name, email, state, country, page, size, sortBy, sortDir))
        );
    }

    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getCustomer(@PathVariable(value= "id") Long id){
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(),"Success",service.get(id))
        );
    }

    
    @PostMapping
    public ResponseEntity<ApiResponse> addCustomer(@Valid @RequestBody CustomerRequestDto customer){
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.CREATED.value(),"Customer Created",service.save(customer))
        );
    }

    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateCustomer(@Valid@PathVariable(value = "id") Long id,
                                                      @RequestBody Customer customer){
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(),"Customer Updated",service.update(id,customer))
        );
    }

    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteCustomer(@PathVariable(value = "id") Long id){
        service.delete(id);
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(),"Customer Deleted",null)
        );
    }
//     @GetMapping("/active")
// public ResponseEntity<ApiResponse> getActiveOrders() {
//     return ResponseEntity.ok(
//         new ApiResponse(200, "Active Orders", service.getActiveOrders())
//     );
// }

    // ─── Bulk Import / Export ─────────────────────────────────────────────────

    @PostMapping(value = "/import/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> importFromExcel(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) return ResponseEntity.badRequest().body(new ApiResponse(400, "File is empty", null));
            Map<String, Object> result = importExportService.importFromExcel(file);
            return ResponseEntity.ok(new ApiResponse(200, "Import completed", result));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ApiResponse(500, "Import failed: " + e.getMessage(), null));
        }
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportToExcel(
            @RequestParam(required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5000") int size) {
        try {
            Page<CustomerResponseDto> data = service.listAll(search, null, null, null, null, page, size, "id", "asc");
            byte[] bytes = importExportService.exportToExcel(data.getContent());
            String filename = "customers_" + LocalDate.now() + ".xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (Exception e) { return ResponseEntity.status(500).build(); }
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportToPdf(
            @RequestParam(required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5000") int size) {
        try {
            Page<CustomerResponseDto> data = service.listAll(search, null, null, null, null, page, size, "id", "asc");
            byte[] bytes = importExportService.exportToPdf(data.getContent());
            String filename = "customers_" + LocalDate.now() + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(bytes);
        } catch (Exception e) { return ResponseEntity.status(500).build(); }
    }

    @GetMapping("/import/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        try {
            byte[] bytes = importExportService.generateExcelTemplate();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"customer_template.xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (Exception e) { return ResponseEntity.status(500).build(); }
    }
}