package com.hepl.product.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hepl.product.Payload.Dto.UserDto.UserRequestDto;
import com.hepl.product.Payload.Dto.UserDto.UserResponseDto;
import com.hepl.product.Payload.Response.ApiResponse;
import com.hepl.product.Service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.Map;
import java.time.LocalDate;
import com.hepl.product.Service.UserImportExportService;
import org.springframework.data.domain.Page;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    @Autowired
    private UserService service;

    @Autowired
    private UserImportExportService importExportService;

    @GetMapping
    public ResponseEntity<ApiResponse> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(),"Success",service.listAll(search, username, email, status, page, size, sortBy, sortDir))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getUser(@PathVariable(value = "id") Integer id){
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(),"Success",service.get(id))
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse> addUser(@RequestBody UserRequestDto dto){
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.CREATED.value(),"User Created",service.save(dto))
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateUser(@PathVariable(value = "id") Integer id, @RequestBody UserRequestDto dto){
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(),"User Updated",service.update(id,dto))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable(value = "id") Integer id){
        service.delete(id);
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(),"User Deleted",null)
        );
    }

    @PostMapping("/{userId}/roles")
    public ResponseEntity<ApiResponse> assignRoles(
            @PathVariable(value = "userId") Integer userId, 
            @RequestBody List<Long> roleIds){
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(),"Roles Assigned",service.assignRoles(userId, roleIds))
        );
    }

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
            Page<UserResponseDto> data = service.listAll(search, null, null, null, page, size, "id", "asc");
            byte[] bytes = importExportService.exportToExcel(data.getContent());
            String filename = "users_" + LocalDate.now() + ".xlsx";
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
            Page<UserResponseDto> data = service.listAll(search, null, null, null, page, size, "id", "asc");
            byte[] bytes = importExportService.exportToPdf(data.getContent());
            String filename = "users_" + LocalDate.now() + ".pdf";
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
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"user_template.xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (Exception e) { return ResponseEntity.status(500).build(); }
    }
}
