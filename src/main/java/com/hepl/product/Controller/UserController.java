package com.hepl.product.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hepl.product.Payload.Response.ApiResponse;
import com.hepl.product.Service.UserService;
import com.hepl.product.model.User;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    @Autowired
    private UserService service;

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
    public ResponseEntity<ApiResponse> addUser(@RequestBody User user){
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.CREATED.value(),"User Created",service.save(user))
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateUser(@PathVariable(value = "id") Integer id, @RequestBody User user){
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(),"User Updated",service.update(id,user))
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
}
