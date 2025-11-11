package com.example.authService.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.authService.service.EmailService;

@RestController
@RequestMapping("/api")
public class UploadController {

    private final Path uploadDir;

    private final EmailService emailService;

    public UploadController(@Value("${app.upload.dir}") String uploadDir, EmailService emailService) throws IOException {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.emailService = emailService;
        Files.createDirectories(this.uploadDir);
    }

    // Only ADMIN or MANAGER can reach this (enforced in SecurityConfig)
    @PostMapping("/upload/file")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                        @AuthenticationPrincipal String username) throws IOException {
        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        if (filename.contains("..")) {
            return ResponseEntity.badRequest().body("Invalid filename");
        }
        Path target = this.uploadDir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return ResponseEntity.ok("Uploaded: " + filename);
    }

    // Send email — allowed roles ADMIN/MANAGER
    @PostMapping("/email/send")
    public ResponseEntity<?> sendEmail(@RequestParam String to, @RequestParam String subject, @RequestParam String body) {
        emailService.sendSimpleMessage(to, subject, body);
        return ResponseEntity.ok("Email sent (or queued) to " + to);
    }

    // Optional: download
    @GetMapping("/download/{filename:.+}")
    public ResponseEntity<?> download(@PathVariable String filename) throws IOException {
        Path filePath = uploadDir.resolve(filename).normalize();
        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }
        byte[] content = Files.readAllBytes(filePath);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(content);
    }
}

