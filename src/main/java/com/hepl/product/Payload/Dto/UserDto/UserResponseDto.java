package com.hepl.product.Payload.Dto.UserDto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class UserResponseDto {
    private int id;
    private String username;
    private String firstname;
    private String lastname;
    private String email;
    private Long phonenumber;
    private String status;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> roles;
}
