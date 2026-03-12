package com.hepl.product.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hepl.product.Payload.Response.ApiResponse;
import com.hepl.product.Service.RoleService;
import com.hepl.product.model.Role;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    @Autowired
    private RoleService service;

    @GetMapping
    public ResponseEntity<ApiResponse> getAllRoles(){
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(),"Success",service.listAll())
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getRole(@PathVariable Long id){
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(),"Success",service.get(id))
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse> addRole(@RequestBody Role role){
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.CREATED.value(),"Role Created",service.save(role))
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateRole(@PathVariable Long id, @RequestBody Role role){
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(),"Role Updated",service.update(id,role))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteRole(@PathVariable Long id){
        service.delete(id);
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(),"Role Deleted",null)
        );
    }

    @PostMapping("/{roleId}/permissions")
    public ResponseEntity<ApiResponse> assignPermissions(
            @PathVariable Long roleId, 
            @RequestBody List<Long> permissionIds){
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(),"Permissions Assigned",service.assignPermissions(roleId, permissionIds))
        );
    }
}
