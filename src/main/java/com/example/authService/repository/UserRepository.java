package com.example.authService.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.authService.entity.AppUser;

@Repository
public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByResetToken(String token);

    // ✅ Added helper for quick checks (used in startup and elsewhere)
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
