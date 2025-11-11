package com.example.authService.service;


import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.authService.entity.AppUser;
import com.example.authService.entity.Role;
import com.example.authService.repository.UserRepository;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repo, PasswordEncoder encoder) {
        this.userRepository = repo;
        this.passwordEncoder = encoder;
    }

    public AppUser createUser(String username,String email, String contactNumber, String rawPassword, Set<Role> roles) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("User exists");
        }
        AppUser u = new AppUser();
        u.setUsername(username);
        u.setEmail(email);
        u.setContactNumber(contactNumber);
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setRoles(roles);
        return userRepository.save(u);
    }

    public Optional<AppUser> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        var authorities = user.getRoles().stream()
                .map(role -> "ROLE_" + role.name().replace("ROLE_", ""))
                .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return User.withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .build();
    }
}
