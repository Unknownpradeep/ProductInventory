package com.hepl.product.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hepl.product.Payload.Response.ApiResponse;
import com.hepl.product.Service.PermissionService;
import com.hepl.product.model.Permission;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    @Autowired
    private PermissionService service;

    @GetMapping
    public ResponseEntity<ApiResponse> getAllPermissions(){
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(),"Success",service.listAll())
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getPermission(@PathVariable(value="id") Long id){
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(),"Success",service.get(id))
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse> addPermission(@RequestBody Permission permission){
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.CREATED.value(),"Permission Created",service.save(permission))
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updatePermission(@PathVariable(value = "id") Long id, @RequestBody Permission permission){
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(),"Permission Updated",service.update(id,permission))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deletePermission(@PathVariable(value = "id") Long id){
        service.delete(id);
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(),"Permission Deleted",null)
        );
    }
}
