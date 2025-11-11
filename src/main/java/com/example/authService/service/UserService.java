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
        <body style="font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f6f8; padding: 30px; margin: 0;">
            <table align="center" width="100%" style="max-width: 600px; background: #ffffff; border-radius: 10px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); overflow: hidden;">
            
            <!-- Header -->
            <tr>
                <td style="background: linear-gradient(135deg, #28a745, #218838); color: white; text-align: center; padding: 20px 0;">
                <h2 style="margin: 0; font-size: 22px;">🎉 Welcome to the Team!</h2>
                </td>
            </tr>

            <!-- Body -->
            <tr>
                <td style="padding: 30px 40px; color: #333333;">
                <h3 style="margin-top: 0;">Hello %s 👋</h3>

                <p style="font-size: 15px; line-height: 1.6;">
                    You’ve been successfully added as a <b>Manager</b> by the Admin.
                </p>

                <p style="font-size: 15px; margin-bottom: 15px;">Here are your login details:</p>

                <table style="border-collapse: collapse; width: 100%; margin-bottom: 20px;">
                    <tr>
                    <td style="padding: 10px; border: 1px solid #ddd; background-color: #f9f9f9; width: 40%; font-weight: bold;">Username:</td>
                    <td style="padding: 10px; border: 1px solid #ddd;">%s</td>
                    </tr>
                    <tr>
                    <td style="padding: 10px; border: 1px solid #ddd; background-color: #f9f9f9; font-weight: bold;">Password:</td>
                    <td style="padding: 10px; border: 1px solid #ddd;">%s</td>
                    </tr>
                </table>

                <p style="font-size: 14px; color: #555; line-height: 1.6;">
                    🔐 For your security, please log in and <b>change your password</b> after your first login.
                </p>

                <p style="margin-top: 25px; font-size: 14px;">
                    Best Regards,<br>
                    <b>Admin Team</b>
                </p>

                <hr style="border: none; border-top: 1px solid #eee; margin: 25px 0;">

                <p style="font-size: 12px; color: #999; text-align: center;">
                    © 2025 Your Company Name. All rights reserved.
                </p>
                </td>
            </tr>
            </table>
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
                <body style="font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f6f8; padding: 30px; margin: 0;">
                    <table align="center" width="100%" style="max-width: 600px; background: #ffffff; border-radius: 10px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); overflow: hidden;">
                    <tr>
                        <td style="background: linear-gradient(135deg, #007bff, #0056b3); color: white; text-align: center; padding: 20px 0;">
                        <h2 style="margin: 0; font-size: 22px;">🔒 Password Reset Request</h2>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding: 30px 40px; color: #333333;">
                        <p style="font-size: 16px;">Hello <strong>%s</strong>,</p>

                        <p style="font-size: 15px; line-height: 1.6;">
                            We received a request to reset your password. Click the button below to reset it.
                        </p>

                        <p style="text-align: center; margin: 30px 0;">
                            <a href="%s" style="background-color: #007bff; color: white; padding: 12px 25px; text-decoration: none; font-size: 16px; border-radius: 6px; display: inline-block;">
                            Reset Password
                            </a>
                        </p>

                        <p style="font-size: 14px; color: #555;">
                            ⏳ This link will expire in <strong>10 minutes</strong>.
                        </p>

                        <p style="font-size: 14px; color: #777; line-height: 1.6;">
                            If you didn’t request this, you can safely ignore this email. Your password will remain unchanged.
                        </p>

                        <hr style="border: none; border-top: 1px solid #eee; margin: 25px 0;">

                        <p style="font-size: 12px; color: #999; text-align: center;">
                            This is an automated message, please do not reply to this email.<br>
                            &copy; 2025 Your Company Name. All rights reserved.
                        </p>
                        </td>
                    </tr>
                    </table>
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
