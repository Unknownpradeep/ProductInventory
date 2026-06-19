package com.hepl.product.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hepl.product.Payload.Dto.AuthDto.AuthResponse;
import com.hepl.product.Payload.Dto.AuthDto.LoginRequest;
import com.hepl.product.Payload.Dto.AuthDto.RegisterRequest;
import com.hepl.product.Payload.Dto.AuthDto.ForgotPasswordRequest;
import com.hepl.product.Payload.Dto.AuthDto.ResetPasswordRequest;
import com.hepl.product.Payload.Response.ApiResponse;
import com.hepl.product.Service.AuthService;

import lombok.RequiredArgsConstructor;

import com.hepl.product.model.Login;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private void logError(Exception ex, String path) {
        ex.printStackTrace();
        try {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            ex.printStackTrace(pw);
            
            java.io.File file = new java.io.File("C:\\Users\\acer\\Desktop\\PRODUCT\\api_error.txt");
            String errorLog = String.format("--- FAILURE LOGGED AT %s ---\nRequest: /api/v1/auth%s\nException Class: %s\nMessage: %s\nStack Trace:\n%s\n\n", 
                java.time.LocalDateTime.now(), path, ex.getClass().getName(), ex.getMessage(), sw.toString());
                
            java.nio.file.Files.writeString(
                file.toPath(), 
                errorLog, 
                java.nio.file.StandardOpenOption.CREATE, 
                java.nio.file.StandardOpenOption.APPEND
            );
        } catch (Exception e) {
            System.err.println("Failed to write to api_error.txt: " + e.getMessage());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            if (response.getToken() == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(HttpStatus.BAD_REQUEST.value(), response.getMessage(), null));
            }
            return ResponseEntity.ok(
                    new ApiResponse(HttpStatus.CREATED.value(), "Registration successful", response));
        } catch (Exception e) {
            logError(e, "/register");
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(
                    new ApiResponse(HttpStatus.OK.value(), "Login successful", response));
        } catch (Exception e) {
            logError(e, "/login");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(HttpStatus.UNAUTHORIZED.value(), e.getMessage(), null));
        }
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse> getAllUsers() {
        return ResponseEntity.ok(
                new ApiResponse(HttpStatus.OK.value(), "Success", authService.getAllUsers()));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse> updateUser(@PathVariable Long id, @RequestBody Login login) {
        try {
            return ResponseEntity.ok(
                    new ApiResponse(HttpStatus.OK.value(), "User updated", authService.updateUser(id, login)));
        } catch (Exception e) {
            logError(e, "/users/" + id);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable Long id) {
        try {
            authService.deleteUser(id);
            return ResponseEntity.ok(
                    new ApiResponse(HttpStatus.OK.value(), "User deleted", null));
        } catch (Exception e) {
            logError(e, "/users/" + id);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse> validateToken(@RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            boolean isValid = authService.validateToken(jwtToken);

            if (isValid) {
                return ResponseEntity.ok(
                        new ApiResponse(HttpStatus.OK.value(), "Token is valid", true));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse(HttpStatus.UNAUTHORIZED.value(), "Invalid token", false));
            }
        } catch (Exception e) {
            logError(e, "/validate");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(HttpStatus.UNAUTHORIZED.value(), "Invalid token", false));
        }
    }

    @PostMapping("/impersonate/{username}")
    public ResponseEntity<ApiResponse> impersonate(@PathVariable String username) {
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(HttpStatus.FORBIDDEN.value(), "Access Denied: Only admins can impersonate users", null));
            }
            AuthResponse response = authService.impersonate(username);
            return ResponseEntity.ok(
                    new ApiResponse(HttpStatus.OK.value(), "Impersonation successful", response));
        } catch (Exception e) {
            logError(e, "/impersonate/" + username);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            authService.forgotPassword(request);
            return ResponseEntity.ok(
                    new ApiResponse(HttpStatus.OK.value(), "Password reset link sent to your email.", null));
        } catch (Exception e) {
            logError(e, "/forgot-password");
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request);
            return ResponseEntity.ok(
                    new ApiResponse(HttpStatus.OK.value(), "Password has been reset successfully.", null));
        } catch (Exception e) {
            logError(e, "/reset-password");
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        }
    }
}
