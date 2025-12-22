package com.skillmap.controller;

import com.skillmap.model.entity.User;
import com.skillmap.repository.UserRepository;
import com.skillmap.service.UserService;
import com.skillmap.service.dto.AuthRequest;
import com.skillmap.service.security.JwtTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtTokenService jwtTokenService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody AuthRequest request) {
        try {
            User user = userService.registerUser(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "name", user.getEmail().split("@")[0]
            ));
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody AuthRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            User user = userService.findByEmail(request.getEmail());

            // Generate JWT token
            UserDetails userDetails = userService.loadUserByUsername(user.getEmail());
            String jwtToken = jwtTokenService.generateToken(userDetails);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", jwtToken);
            response.put("user", Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "name", user.getEmail().split("@")[0]
            ));
            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Invalid credentials");
            return ResponseEntity.status(401).body(errorResponse);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            // In a stateless JWT system, logout is handled client-side by removing the token
            // Server-side, we could implement token blacklisting if needed
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Logged out successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Logout failed");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            String username = jwtTokenService.extractUsername(token);

            if (username != null && jwtTokenService.validateToken(token)) {
                User user = userService.findByEmail(username);
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("user", Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "name", user.getEmail().split("@")[0]
                ));
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Invalid or expired token");
                return ResponseEntity.status(401).body(errorResponse);
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Token validation failed");
            return ResponseEntity.status(401).body(errorResponse);
        }
    }
}
