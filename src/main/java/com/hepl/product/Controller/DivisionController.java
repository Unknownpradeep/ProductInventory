package com.hepl.product.Controller;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.time.LocalDate;
import java.util.Map;
import com.hepl.product.Service.DivisionImportExportService;
import com.hepl.product.Payload.Dto.DivisionDTO.DivisionResponseDto;
import org.springframework.data.domain.Page;

import com.hepl.product.Payload.Dto.DivisionDTO.DivisionRequestDto;
import com.hepl.product.Payload.Response.ApiResponse;
import com.hepl.product.Service.DivisionService;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/divisions")
@RequiredArgsConstructor
public class DivisionController {
    @Autowired
    private DivisionService service;

    @Autowired
    private DivisionImportExportService importExportService;

    @GetMapping
    public ResponseEntity<ApiResponse> getAllDivisions(
        @RequestParam(required = false) String search,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "10") int size,
        @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,
        @RequestParam(value = "sortDir", defaultValue = "asc") String sortDir
    ){
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(),"Success",service.listAll(search, page, size, sortBy, sortDir))
        );
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getDivision(@Valid @PathVariable(value = "id") Long id){
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(),"Success",service.get(id)));
    }
    @PostMapping
    public ResponseEntity<?> addDivision(@Valid @RequestBody DivisionRequestDto division){
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(),"Success",service.save(division)));

        }
     @PutMapping("/{id}")
     public ResponseEntity<ApiResponse> updateDivision(@Valid @PathVariable(value = "id") Long id,@RequestBody DivisionRequestDto division ) {
        return ResponseEntity.ok(
         new ApiResponse(HttpStatus.OK.value(),"Success",service.update(id, division)));
     }
     @DeleteMapping("/{id}")
      public ResponseEntity<ApiResponse> deleteDivision(@PathVariable(value = "id") Long id){
        service.delete(id);
        return ResponseEntity.ok(
         new ApiResponse(HttpStatus.OK.value(),"Division deleted successfully",null));
    }

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

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportToExcel(
            @RequestParam(required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5000") int size) {
        try {
            Page<DivisionResponseDto> divisionPage = service.listAll(search, page, size, "id", "asc");
            byte[] bytes = importExportService.exportToExcel(divisionPage.getContent());
            String filename = "divisions_export_" + LocalDate.now() + ".xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportToPdf(
            @RequestParam(required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5000") int size) {
        try {
            Page<DivisionResponseDto> divisionPage = service.listAll(search, page, size, "id", "asc");
            byte[] bytes = importExportService.exportToPdf(divisionPage.getContent());
            String filename = "divisions_report_" + LocalDate.now() + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/import/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        try {
            byte[] bytes = importExportService.generateExcelTemplate();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"division_import_template.xlsx\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
