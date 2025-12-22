package com.skillmap.service;

import com.skillmap.model.entity.User;
import com.skillmap.repository.UserRepository;
import com.skillmap.service.dto.AuthRequest;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final long jwtExpirationMs = 86400000; // 1 day

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    public User registerUser(AuthRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use");
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setIsActive(true);
        return userRepository.save(user);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public String generateToken(User user) {
        return Jwts.builder()
            .setSubject(user.getEmail())
            .setIssuedAt(new Date())
            .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
            .signWith(Keys.hmacShaKeyFor(getSecretKey()), Jwts.SIG.HS512)
            .compact();
    }

    private byte[] getSecretKey() {
        // Expect a Base64-encoded 64-byte secret. If not provided, throw to avoid insecure defaults.
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret not configured. Set security.jwt.secret");
        }
        byte[] raw = jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        // If the provided secret is not long enough for HS512, derive a key via hashing
        if (raw.length < 64) {
            try {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-512");
                return md.digest(raw);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to derive JWT key", e);
            }
        }
        return raw;
    }

    public User findById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = findByEmail(username);
        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())
            .password(user.getPassword())
            .roles("USER")
            .build();
    }
}
