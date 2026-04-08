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

}