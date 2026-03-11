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
import org.springframework.web.bind.annotation.RestController;


import com.hepl.product.Payload.Response.ApiResponse;
import com.hepl.product.Service.CategoryService;
import com.hepl.product.model.Category;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {
    @Autowired
    private CategoryService service;

    @GetMapping
    public ResponseEntity<ApiResponse> getAllCategories(){
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(),"Success",service.listAll())
        );
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getCategory(@PathVariable Long id){
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(),"Success",service.get(id)));
    }
    @PostMapping
    public ResponseEntity<?> addCategory(@Valid @RequestBody Category category){
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(),"Success",service.save(category)));

        }
     @PutMapping("/{id}")
     public ResponseEntity<ApiResponse> updateCategory(@Valid @PathVariable Long id,@RequestBody Category category ) {
        return ResponseEntity.ok(
         new ApiResponse(HttpStatus.OK.value(),"Success",service.update(id, category)));
     }
     @DeleteMapping("/{id}")
      public ResponseEntity<ApiResponse> deleteCategory(@PathVariable Long id){
        service.delete(id);
        return ResponseEntity.ok(
         new ApiResponse(HttpStatus.OK.value(),"Category deleted successfully",null));
     

    }
   
    

    }



