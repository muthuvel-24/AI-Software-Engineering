package com.assistant.ai.controller;

import com.assistant.ai.dto.AuthRequest;
import com.assistant.ai.dto.AuthResponse;
import com.assistant.ai.dto.RegisterRequest;
import com.assistant.ai.model.User;
import com.assistant.ai.repository.UserRepository;
import com.assistant.ai.security.JwtUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error: Email is already in use!"));
        }

        User user = User.builder()
                .email(signUpRequest.getEmail())
                .password(passwordEncoder.encode(signUpRequest.getPassword()))
                .name(signUpRequest.getName())
                .build();

        userRepository.save(user);

        String jwt = jwtUtils.generateJwtToken(user.getEmail());
        return ResponseEntity.ok(AuthResponse.builder()
                .token(jwt)
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build());
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody AuthRequest loginRequest) {
        Optional<User> userOptional = userRepository.findByEmail(loginRequest.getEmail());

        if (userOptional.isEmpty() || !passwordEncoder.matches(loginRequest.getPassword(), userOptional.get().getPassword())) {
            return ResponseEntity.status(401).body(Map.of("message", "Error: Invalid email or password!"));
        }

        User user = userOptional.get();
        String jwt = jwtUtils.generateJwtToken(user.getEmail());

        return ResponseEntity.ok(AuthResponse.builder()
                .token(jwt)
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build());
    }

    @PostMapping("/google-login")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String name = request.get("name");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error: Invalid Google payload"));
        }

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode("GoogleOAuthStubPassword123!"))
                    .name(name != null ? name : "Google User")
                    .build();
            return userRepository.save(newUser);
        });

        String jwt = jwtUtils.generateJwtToken(user.getEmail());
        return ResponseEntity.ok(AuthResponse.builder()
                .token(jwt)
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build());
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Error: Not authenticated"));
        }
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            User user = (User) principal;
            return ResponseEntity.ok(Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "name", user.getName(),
                    "createdAt", user.getCreatedAt()
            ));
        }
        return ResponseEntity.status(401).body(Map.of("message", "Error: Not authenticated"));
    }
}
