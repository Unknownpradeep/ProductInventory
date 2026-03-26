package com.hepl.product.Payload.Dto.CustomerDto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerRequestDto {
    @NotBlank(message = "Customer name is required")
    private String name;
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;
    @NotBlank(message = "Address is required")
    private String address;
    
    @NotBlank(message = "State is required")
    private String state;
    @NotBlank(message = "Country is required")
    private String country;
    @NotBlank(message = "Pincode is required")
    private String pincode;                                        
}
