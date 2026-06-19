package com.hepl.product.Service;

import com.hepl.product.Payload.Dto.AuthDto.AuthResponse;
import com.hepl.product.Payload.Dto.AuthDto.LoginRequest;
import com.hepl.product.Payload.Dto.AuthDto.RegisterRequest;

import com.hepl.product.Payload.Dto.AuthDto.ForgotPasswordRequest;
import com.hepl.product.Payload.Dto.AuthDto.ResetPasswordRequest;

import java.util.List;
import com.hepl.product.model.Login;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    boolean validateToken(String token);
    List<Login> getAllUsers();
    Login updateUser(Long id, Login updated);
    void deleteUser(Long id);
    AuthResponse impersonate(String username);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
}
