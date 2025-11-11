package com.example.authService;

import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.authService.entity.AppUser;
import com.example.authService.entity.Role;
import com.example.authService.repository.UserRepository;

@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner init(UserRepository userRepository, PasswordEncoder encoder) {
        return args -> {
            // Check if admin already exists
            if (!userRepository.existsByUsername("Isha2504")) {
                AppUser admin = new AppUser();
                admin.setUsername("Isha2504");
                admin.setPassword(encoder.encode("Isha123"));
                admin.setEmail("ishathakur2504@gmail.com");
                admin.setContactNumber("7972460819");
                admin.setRoles(Set.of(Role.ROLE_ADMIN));

                userRepository.save(admin);

                System.out.println("\n✅ Default admin created successfully!");
                System.out.println("👉 Username: Isha2504");
                System.out.println("👉 Password: Isha123");
                System.out.println("👉 Role: ADMIN\n");
            } else {
                System.out.println("ℹ️ Admin user already exists. Skipping default admin creation.");
            }
        };
    }
}
