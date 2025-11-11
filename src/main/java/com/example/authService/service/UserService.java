package com.example.authService.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.authService.entity.AppUser;
import com.example.authService.entity.Role;
import com.example.authService.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public Optional<AppUser> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<AppUser> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public void createUser(String username, String password, String email, String contactNumber, Set<Role> roles) {
    if (userRepository.findByUsername(username).isPresent()) {
        throw new RuntimeException("Username already exists");
    }
    if (userRepository.findByEmail(email).isPresent()) {
        throw new RuntimeException("Email already exists");
    }

    AppUser user = new AppUser();
    user.setUsername(username);
    user.setPassword(passwordEncoder.encode(password));
    user.setEmail(email);
    user.setContactNumber(contactNumber);
    user.setRoles(roles);
    userRepository.save(user);

    // ✅ Send welcome HTML email
    String subject = "Welcome to the Admin Portal";
    String htmlMessage = """
        <html>
        <body style="font-family: Arial, sans-serif; color: #333;">
            <h2>Welcome, %s 👋</h2>
            <p>You’ve been added as a <b>Manager</b> by the Admin.</p>
            <p>Here are your login details:</p>
            <table style="border-collapse: collapse;">
                <tr>
                    <td style="padding: 6px;"><b>Username:</b></td>
                    <td style="padding: 6px;">%s</td>
                </tr>
                <tr>
                    <td style="padding: 6px;"><b>Password:</b></td>
                    <td style="padding: 6px;">%s</td>
                </tr>
            </table>
            <p>Please log in and change your password after first use for security reasons.</p>
            <br>
            <p>Best Regards,<br>Admin Team</p>
        </body>
        </html>
        """.formatted(username, username, password);

    emailService.sendHtmlMessage(email, subject, htmlMessage);
}



    // Generate token and send reset mail
    public void initiatePasswordReset(String email) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(10)); // valid for 10 mins
        userRepository.save(user);

        String resetLink = "http://localhost:8080/reset-password.html?token=" + token;
        String subject = "Password Reset Request";
        String message = """
                <html>
                <body style="font-family: Arial, sans-serif;">
                    <h3>Password Reset Request</h3>
                    <p>Hello %s,</p>
                    <p>We received a request to reset your password. Click the button below to proceed:</p>
                    <p>
                        <a href="%s" style="background-color:#007bff;color:white;
                        padding:10px 15px;text-decoration:none;border-radius:5px;">Reset Password</a>
                    </p>
                    <p>This link is valid for 10 minutes.</p>
                    <p>If you didn’t request this, just ignore this email.</p>
                </body>
                </html>
                """.formatted(user.getUsername(), resetLink);

        emailService.sendHtmlMessage(email, subject, message);

    }

    // Complete reset process
    public void resetPassword(String token, String newPassword) {
        AppUser user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Reset token has expired");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    public void saveUser(AppUser user) {
        userRepository.save(user);
    }

}
