package com.hepl.product.Service.ServiceImpl;

import java.time.LocalDateTime;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.hepl.product.Payload.Dto.AuthDto.AuthResponse;
import com.hepl.product.Payload.Dto.AuthDto.LoginRequest;
import com.hepl.product.Payload.Dto.AuthDto.RegisterRequest;
import com.hepl.product.Repository.LoginRepository;
import com.hepl.product.Service.AuthService;
import com.hepl.product.Util.JwtUtil;
import com.hepl.product.model.Login;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final LoginRepository loginRepository;
    private final JwtUtil jwtUtil;
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
        login.setRole(request.getRole() != null ? request.getRole() : "USER");
        login.setActive(true);
        login.setCreatedAt(LocalDateTime.now());

        loginRepository.save(login);

        String token = jwtUtil.generateToken(login.getUsername());

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

        String token = jwtUtil.generateToken(login.getUsername());

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
}
