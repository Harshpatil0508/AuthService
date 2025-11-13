package com.example.authService.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.authService.security.JwtUtil;
import com.example.authService.service.UserService;

@Configuration
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    public SecurityConfig(JwtUtil jwtUtil, UserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter() {
        return new JwtAuthFilter(jwtUtil, userService);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());
        http.cors(cors -> {}); // Enable CORS if needed for frontend

        http.authorizeHttpRequests(auth -> auth
            // 🔓 Public endpoints
            .requestMatchers(
                "/api/auth/login",
                "/api/auth/register",
                "/api/auth/forgot-password",
                "/api/auth/reset-password",
                "/reset-password.html"
            ).permitAll()

            // 🔐 Admin-only operations
            .requestMatchers(
                "/api/auth/add-user",
                "/api/auth/approve-user",
                "/api/auth/reject-user"
            ).hasAuthority("ROLE_ADMIN")

            // 🔐 Authenticated users (Admin, Manager, User)
            .requestMatchers("/api/auth/change-password").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_USER")

            // 🔒 Everything else must be authenticated
            .anyRequest().authenticated()
        );

        http.addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
