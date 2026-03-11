package com.hepl.product.Controller;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hepl.product.Payload.Dto.ProductDto.ProductRequestDto;
import com.hepl.product.Payload.Response.ApiResponse;
import com.hepl.product.Service.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
@RequestMapping("/api/v1/product")
@RequiredArgsConstructor
public class ProductController {
     @Autowired
     private  ProductService service;

     @GetMapping
     public ResponseEntity<ApiResponse> getAllProducts(){
        return ResponseEntity.ok(
         new ApiResponse(HttpStatus.OK.value(),"Success",service.listAll()));
     }

     @GetMapping("/{id}")
     public ResponseEntity<ApiResponse>getProduct(@PathVariable Long id) {
       return ResponseEntity.ok(
         new ApiResponse(HttpStatus.OK.value(),"Success",service.get(id)));
     }

     @PostMapping
     public ResponseEntity<?>addProduct(@Valid @RequestBody  ProductRequestDto dto) {
        return ResponseEntity.ok(
         new ApiResponse(HttpStatus.OK.value(),"Success",service.save(dto)));
     }

     @PutMapping("/{id}")
     public ResponseEntity<ApiResponse>updateProduct(@Valid @PathVariable Long id,@RequestBody ProductRequestDto dto ) {
        return ResponseEntity.ok(
         new ApiResponse(HttpStatus.OK.value(),"Success",service.update(id, dto)));
     }
     @DeleteMapping("/{id}")
      public ResponseEntity<ApiResponse> deleteProduct(@PathVariable Long id){
        service.delete(id);
        return ResponseEntity.ok(
         new ApiResponse(HttpStatus.OK.value(),"Product deleted successfully",null));
}
        @GetMapping("/category/{categoryId}")
      public ResponseEntity<ApiResponse> getByCategory(@PathVariable Long categoryId) {
          return ResponseEntity.ok(
          new ApiResponse(HttpStatus.OK.value(),"Success",service.findByCategory(categoryId)));
      }
        
      @GetMapping("/customer/{customerId}")
      public ResponseEntity<ApiResponse> getByCustomer(@PathVariable Long customerId) {
          return ResponseEntity.ok(
          new ApiResponse(HttpStatus.OK.value(),"Success",service.findByCustomer(customerId)));
      }
      
     

     
}
