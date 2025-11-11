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
        http.csrf(c -> c.disable());

        http.authorizeHttpRequests(auth -> auth
            // 🔓 Public endpoints (no authentication needed)
            .requestMatchers(
                "/api/auth/login",
                "/api/auth/forgot-password",
                "/api/auth/reset-password",
                "/reset-password.html" // in case you serve static reset page from backend
            ).permitAll()

            // 🔐 Admin-only
            .requestMatchers("/api/auth/add-manager").hasAuthority("ROLE_ADMIN")

            // 🔐 Logged-in users (either Admin or Manager)
            .requestMatchers("/api/auth/change-password").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER")
            .requestMatchers("/api/upload/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER")
            .requestMatchers("/api/email/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER")

            // 🔒 Everything else requires authentication
            .anyRequest().authenticated()
        );

        http.addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
