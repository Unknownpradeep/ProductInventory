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

    @GetMapping
    public ResponseEntity<ApiResponse> getAllDivisions(){
        return ResponseEntity.ok(
            new ApiResponse(HttpStatus.OK.value(),"Success",service.listAll())
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
   
    

    }



