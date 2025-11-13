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

    // ✅ Create Manager (by Admin)
    public void createUser(String username, String password, String email, String contactNumber,
                       String employeeId, String designation, Set<Role> roles) {
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
        user.setEmployeeId(employeeId);
        user.setDesignation(designation);
        user.setRoles(roles);
        user.setApproved(true); // Manager created by admin → auto-approved
        userRepository.save(user);

        // ✅ Send welcome email
        String subject = "Welcome to the Admin Portal";
        String htmlMessage = """
            <html>
            <body style="font-family: Arial, sans-serif; color: #333;">
                <h2>Welcome, %s 👋</h2>
                <p>You’ve been added as a <b>Manager</b> by the Admin.</p>
                <p>Here are your login details:</p>
                <table style="border-collapse: collapse;">
                    <tr><td><b>Username:</b></td><td>%s</td></tr>
                    <tr><td><b>Password:</b></td><td>%s</td></tr>
                </table>
                <p>Please change your password after your first login.</p>
                <br><p>Best Regards,<br>Admin Team</p>
            </body>
            </html>
        """.formatted(username, username, password);

        emailService.sendHtmlMessage(email, subject, htmlMessage);
    }

    // ✅ New: User Registration (requires admin approval)
    public void registerUser(String username, String password, String email, String contactNumber,
                             String employeeId, String designation) {

        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setContactNumber(contactNumber);
        user.setEmployeeId(employeeId != null ? employeeId : "EMP-" + System.currentTimeMillis());
        user.setDesignation(designation);
        user.setRoles(Set.of(Role.ROLE_USER));
        user.setApproved(false); // admin approval pending

        userRepository.save(user);

        // ✅ Notify user
        String subject = "Registration Received - Pending Approval";
        String message = """
            <html>
            <body style="font-family: Arial, sans-serif;">
                <h3>Registration Submitted</h3>
                <p>Dear %s,</p>
                <p>Thank you for registering! Your account is pending admin approval.</p>
                <p>You’ll receive an email once your request has been approved or rejected.</p>
                <p>Employee ID: <b>%s</b><br>
                Designation: <b>%s</b></p>
            </body>
            </html>
        """.formatted(username, user.getEmployeeId(), designation);

        emailService.sendHtmlMessage(email, subject, message);
    }

    // ✅ Approve or Reject registration
    public void updateApprovalStatus(Long userId, boolean approve) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setApproved(approve);
        userRepository.save(user);

        String subject = approve ? "Registration Approved 🎉" : "Registration Rejected ❌";
        String message;

        if (approve) {
            message = """
                <html>
                <body>
                    <p>Dear %s,</p>
                    <p>Your account has been <b>approved</b> by the admin.</p>
                    <p>You can now log in using your registered credentials.</p>
                </body>
                </html>
            """.formatted(user.getUsername());
        } else {
            message = """
                <html>
                <body>
                    <p>Dear %s,</p>
                    <p>We regret to inform you that your registration has been <b>rejected</b> by the admin.</p>
                </body>
                </html>
            """.formatted(user.getUsername());
        }

        emailService.sendHtmlMessage(user.getEmail(), subject, message);
    }

    // ✅ Forgot password
    public void initiatePasswordReset(String email) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        String resetLink = "http://localhost:8080/reset-password.html?token=" + token;
        String subject = "Password Reset Request";
        String message = """
            <html>
            <body style="font-family: Arial, sans-serif;">
                <h3>Password Reset</h3>
                <p>Hello %s,</p>
                <p>Click below to reset your password (valid for 10 mins):</p>
                <a href="%s" style="background-color:#007bff;color:white;padding:10px 15px;text-decoration:none;border-radius:5px;">Reset Password</a>
            </body>
            </html>
        """.formatted(user.getUsername(), resetLink);

        emailService.sendHtmlMessage(email, subject, message);
    }

    // ✅ Reset password logic
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
    // ✅ Create Pending User (User Registration)
    public void createPendingUser(String username, String password, String email, String contactNumber,
                        String employeeId, String designation, Set<Role> roles) {
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
        user.setEmployeeId(employeeId);
        user.setDesignation(designation);
        user.setRoles(roles);
        user.setApproved(false); // pending admin approval
        userRepository.save(user);

        // Send mail to user
        String subject = "Registration Received - Pending Approval";
        String html = """
            <html>
            <body style="font-family: Arial, sans-serif;">
                <h3>Welcome, %s!</h3>
                <p>Thank you for registering. Your account is pending admin approval.</p>
                <p>You will receive an email once your registration is approved or rejected.</p>
                <br><p>Regards,<br>Admin Team</p>
            </body>
            </html>
        """.formatted(username);

        emailService.sendHtmlMessage(email, subject, html);
    }

    // ✅ Approve User
    public AppUser approveUser(String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setApproved(true);
        userRepository.save(user);
        return user;
    }

    // ✅ Reject User
    public AppUser rejectUser(String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setApproved(false);
        userRepository.save(user);
        return user;
    }

}

