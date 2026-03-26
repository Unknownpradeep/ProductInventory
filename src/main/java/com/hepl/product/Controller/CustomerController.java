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

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    @Autowired
    private CustomerService service;

    
    @GetMapping
    public ResponseEntity<ApiResponse> getAllCustomers(){
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(),"Success",service.listAll())
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

}