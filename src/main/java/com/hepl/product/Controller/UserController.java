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
    public ResponseEntity<ApiResponse> getAllUsers(){
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(),"Success",service.listAll())
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getUser(@PathVariable Integer id){
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
    public ResponseEntity<ApiResponse> updateUser(@PathVariable Integer id, @RequestBody User user){
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(),"User Updated",service.update(id,user))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable Integer id){
        service.delete(id);
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(),"User Deleted",null)
        );
    }

    @PostMapping("/{userId}/roles")
    public ResponseEntity<ApiResponse> assignRoles(
            @PathVariable Integer userId, 
            @RequestBody List<Long> roleIds){
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(),"Roles Assigned",service.assignRoles(userId, roleIds))
        );
    }
}
