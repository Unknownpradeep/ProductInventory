package com.hepl.product.Service.ServiceImpl;

import java.time.LocalDateTime;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.hepl.product.Payload.Dto.AuthDto.AuthResponse;
import com.hepl.product.Payload.Dto.AuthDto.LoginRequest;
import com.hepl.product.Payload.Dto.AuthDto.RegisterRequest;
import com.hepl.product.Repository.LoginRepository;
import com.hepl.product.Service.AuthService;
import com.hepl.product.Util.JwtUtil;
import com.hepl.product.model.Login;

import com.hepl.product.Payload.Dto.AuthDto.ForgotPasswordRequest;
import com.hepl.product.Payload.Dto.AuthDto.ResetPasswordRequest;
import com.hepl.product.Service.EmailService;
import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final LoginRepository loginRepository;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public AuthResponse register(RegisterRequest request) {
        
        if (loginRepository.existsByUsername(request.getUsername())) {
            return new AuthResponse(null, null, null, null, "Username already exists");
        }
        
        if (loginRepository.existsByEmail(request.getEmail())) {
            return new AuthResponse(null, null, null, null, "Email already exists");
        }

        Login login = new Login();
        login.setUsername(request.getUsername());
        login.setPassword(passwordEncoder.encode(request.getPassword()));
        login.setEmail(request.getEmail());
        //login.setRole(request.getRole() != null ? request.getRole() : "USER");
        login.setActive(true);
        login.setCreatedAt(LocalDateTime.now());
        login.setDeleted(false);
        login.setRole(request.getRole() != null && !request.getRole().isBlank() ? request.getRole() : "ROLE_VIEWER");

        loginRepository.save(login);

        String token = jwtUtil.generateToken(login.getUsername(), login.getRole());

        return new AuthResponse(
            token,
            login.getUsername(),
            login.getEmail(),
            login.getRole(),
            "Registration successful"
        );
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        
        Login login = loginRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), login.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        if (!login.isActive()) {
            throw new RuntimeException("Account is inactive");
        }

        login.setLastLogin(LocalDateTime.now());
        loginRepository.save(login);

        String token = jwtUtil.generateToken(login.getUsername(), login.getRole());

        return new AuthResponse(
            token,
            login.getUsername(),
            login.getEmail(),
            login.getRole(),
            "Login successful"
        );
    }

    @Override
    public boolean validateToken(String token) {
        try {
            String username = jwtUtil.extractUsername(token);
            return jwtUtil.validateToken(token, username);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<Login> getAllUsers() {
        return loginRepository.findByDeletedFalse();
    }

    @Override
    public Login updateUser(Long id, Login updated) {
        Login existing = loginRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (updated.getUsername() != null && !updated.getUsername().isBlank())
            existing.setUsername(updated.getUsername());
        if (updated.getEmail() != null && !updated.getEmail().isBlank())
            existing.setEmail(updated.getEmail());
        if (updated.getRole() != null && !updated.getRole().isBlank())
            existing.setRole(updated.getRole());
        if (updated.getPassword() != null && !updated.getPassword().isBlank())
            existing.setPassword(passwordEncoder.encode(updated.getPassword()));
        existing.setActive(updated.isActive());
        return loginRepository.save(existing);
    }

    @Override
    public void deleteUser(Long id) {
        Login existing = loginRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        existing.setDeleted(true);
        loginRepository.save(existing);
    }

    @Override
    public AuthResponse impersonate(String username) {
        Login login = loginRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (login.isDeleted()) {
            throw new RuntimeException("Cannot impersonate a deleted user");
        }

        if (!login.isActive()) {
            throw new RuntimeException("Cannot impersonate an inactive user");
        }

        String token = jwtUtil.generateToken(login.getUsername(), login.getRole());

        return new AuthResponse(
            token,
            login.getUsername(),
            login.getEmail(),
            login.getRole(),
            "Impersonation successful"
        );
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        Login login = loginRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User with this email does not exist"));

        String token = UUID.randomUUID().toString();
        login.setResetPasswordToken(token);
        login.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(15));
        loginRepository.save(login);

        // Send reset email using emailService
        String resetUrl = "http://localhost:5173/reset-password?token=" + token;
        
        String subject = "Password Reset Request";
        String htmlBody = buildResetEmailBody(login.getUsername(), resetUrl);
        
        emailService.sendEmail(login.getEmail(), subject, htmlBody);
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        Login login = loginRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Invalid or expired password reset token"));

        if (login.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Password reset token has expired");
        }

        // Validate password strength:
        // - At least 8 characters
        // - At least one uppercase letter
        // - At least one lowercase letter
        // - At least one digit
        // - At least one special character
        String newPassword = request.getNewPassword();
        if (newPassword == null || newPassword.isBlank()) {
            throw new RuntimeException("New password cannot be empty");
        }

        String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$";
        if (!newPassword.matches(passwordRegex)) {
            throw new RuntimeException("Password must be at least 8 characters long, and contain at least one uppercase letter, one lowercase letter, one digit, and one special character (@$!%*?&#).");
        }

        // Update password and clear reset token
        login.setPassword(passwordEncoder.encode(newPassword));
        login.setResetPasswordToken(null);
        login.setResetPasswordTokenExpiry(null);
        loginRepository.save(login);
    }

    private String buildResetEmailBody(String username, String resetUrl) {
        return "<div style=\"font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 600px; margin: 30px auto; padding: 24px 30px; background-color: #ffffff; color: #334155; border-radius: 6px; border: 1px solid #e2e8f0;\">" +
                "  <h2 style=\"color: #1e3a8a; font-weight: 700; font-size: 20px; margin-top: 0; margin-bottom: 16px;\">Password Reset Request</h2>" +
                "  <p style=\"color: #334155; font-size: 14px; line-height: 1.6; margin: 0 0 12px 0;\">Hello " + username + ",</p>" +
                "  <p style=\"color: #334155; font-size: 14px; line-height: 1.6; margin: 0 0 20px 0;\">We received a request to reset your password for your Product Inventory account. Click the button below to choose a new password. This link will expire in 15 minutes.</p>" +
                "  <div style=\"margin: 24px 0; text-align: center;\">" +
                "    <a href=\"" + resetUrl + "\" style=\"display: inline-block; padding: 10px 20px; background-color: #1e3a8a; color: #ffffff; text-decoration: none; font-weight: 700; border-radius: 4px; font-size: 13px; text-align: center;\">Reset Password</a>" +
                "  </div>" +
                "  <p style=\"color: #64748b; font-size: 13px; line-height: 1.6; margin: 20px 0 0 0;\">If you did not request a password reset, please ignore this email or contact support if you have questions.</p>" +
                "  <hr style=\"border: none; border-top: 1px solid #e2e8f0; margin: 24px 0;\" />" +
                "  <p style=\"color: #64748b; font-size: 11px; text-align: center; margin: 0;\">Product Inventory Management System &bull; Secured with TLS</p>" +
                "</div>";
    }
}
