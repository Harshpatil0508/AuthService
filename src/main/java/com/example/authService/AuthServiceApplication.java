package com.example.authService;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.example.authService.entity.AppUser;
import com.example.authService.entity.Role;
import com.example.authService.repository.UserRepository;

import java.util.Set;

@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner init(UserRepository userRepository, PasswordEncoder encoder) {
        return args -> {
            if (!userRepository.existsByUsername("Isha2504")) {
                AppUser admin = new AppUser();
                admin.setUsername("Isha2504");
                admin.setPassword(encoder.encode("Isha123"));
                admin.setEmail("ishathakur@admin.com");
                admin.setContactNumber("7972460819");
                admin.setRoles(Set.of(Role.ROLE_ADMIN));
                userRepository.save(admin);
                System.out.println("✅ Default admin created: username=admin, password=admin123");
            }
        };
    }
}
