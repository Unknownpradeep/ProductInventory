package com.hepl.product.Payload.Dto.AuthDto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String email;
    //private String role;
}
