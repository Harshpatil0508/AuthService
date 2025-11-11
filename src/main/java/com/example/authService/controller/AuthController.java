package com.example.authService.controller;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.example.authService.dto.AuthRequest;
import com.example.authService.dto.AuthResponse;
import com.example.authService.dto.RegisterRequest;
import com.example.authService.entity.AppUser;
import com.example.authService.entity.Role;
import com.example.authService.security.JwtUtil;
import com.example.authService.service.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    // ✅ LOGIN ENDPOINT
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        AppUser user = userService.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
        Set<String> roles = user.getRoles().stream().map(Enum::name).collect(Collectors.toSet());
        String token = jwtUtil.generateToken(user.getUsername(), roles);
        return ResponseEntity.ok(new AuthResponse(token));
    }

    // ✅ ADMIN-ONLY ENDPOINT TO ADD MANAGER
    @PostMapping("/add-manager")
    public ResponseEntity<?> addManager(@RequestBody RegisterRequest request) {
        userService.createUser(
                request.getUsername(),
                request.getPassword(),
                request.getEmail(),
                request.getContactNumber(),
                Set.of(Role.ROLE_MANAGER)
        );
        return ResponseEntity.ok("Manager added successfully");
    }
}
