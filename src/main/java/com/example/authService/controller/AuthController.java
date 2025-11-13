package com.example.authService.controller;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.authService.dto.AuthRequest;
import com.example.authService.dto.AuthResponse;
import com.example.authService.dto.RegisterRequest;
import com.example.authService.entity.AppUser;
import com.example.authService.entity.Role;
import com.example.authService.security.JwtUtil;
import com.example.authService.service.EmailService;
import com.example.authService.service.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    public AuthController(UserService userService, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, EmailService emailService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
    }

    // ✅ LOGIN ENDPOINT
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        AppUser user = userService.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        if (!user.isApproved()) {
            return ResponseEntity.status(403).body("Your account is pending admin approval.");
        }

        Set<String> roles = user.getRoles().stream().map(Enum::name).collect(Collectors.toSet());
        String token = jwtUtil.generateToken(user.getUsername(), roles);

        return ResponseEntity.ok(new AuthResponse(token));
    }

    // ✅ ADMIN ADD MANAGER (immediate approval)
    @PostMapping("/add-user")
    public ResponseEntity<?> addManager(@RequestBody RegisterRequest request) {
        userService.createUser(
                request.getUsername(),
                request.getPassword(),
                request.getEmail(),
                request.getContactNumber(),
                request.getEmployeeId(),
                request.getDesignation(),
                Set.of(Role.ROLE_MANAGER)
        );
        return ResponseEntity.ok("User added successfully");
    }

    // ✅ USER REGISTRATION (Pending Approval)
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest request) {
        userService.createPendingUser(
                request.getUsername(),
                request.getPassword(),
                request.getEmail(),
                request.getContactNumber(),
                request.getEmployeeId(),
                request.getDesignation(),
                Set.of(Role.ROLE_USER)
        );
        return ResponseEntity.ok("Registration successful. Awaiting admin approval.");
    }

    // ✅ ADMIN APPROVES USER
    @PostMapping("/approve-user")
    public ResponseEntity<?> approveUser(@RequestParam String username) {
        AppUser user = userService.approveUser(username);
        return ResponseEntity.ok("User approved successfully and email sent to " + user.getEmail());
    }

    // ✅ ADMIN REJECTS USER
    @PostMapping("/reject-user")
    public ResponseEntity<?> rejectUser(@RequestParam String username) {
        AppUser user = userService.rejectUser(username);
        emailService.sendEmail(
                user.getEmail(),
                "Account Rejected",
                "Dear " + user.getUsername() + ",\n\nWe regret to inform you that your registration has been rejected by the admin."
        );
        return ResponseEntity.ok("User rejected successfully and email sent.");
    }

    // ✅ FORGOT PASSWORD
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String email) {
        userService.initiatePasswordReset(email);
        return ResponseEntity.ok("Password reset link sent to " + email);
    }

    // ✅ RESET PASSWORD
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestParam String token, @RequestParam String newPassword) {
        userService.resetPassword(token, newPassword);
        return ResponseEntity.ok("Password has been reset successfully.");
    }

    // ✅ CHANGE PASSWORD
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestParam String username,
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {

        AppUser user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return ResponseEntity.status(400).body("Old password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userService.saveUser(user);

        return ResponseEntity.ok("Password changed successfully");
    }
}
