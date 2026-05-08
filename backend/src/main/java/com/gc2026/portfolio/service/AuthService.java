package com.gc2026.portfolio.service;

import com.gc2026.portfolio.domain.entity.Category;
import com.gc2026.portfolio.domain.entity.User;
import com.gc2026.portfolio.domain.enums.CategoryType;
import com.gc2026.portfolio.domain.enums.UserRole;
import com.gc2026.portfolio.domain.exception.ResourceNotFoundException;
import com.gc2026.portfolio.domain.exception.ValidationException;
import com.gc2026.portfolio.dto.request.LoginRequest;
import com.gc2026.portfolio.dto.request.RegisterRequest;
import com.gc2026.portfolio.dto.response.AuthResponse;
import com.gc2026.portfolio.dto.response.UserResponse;
import com.gc2026.portfolio.repository.CategoryRepository;
import com.gc2026.portfolio.repository.UserRepository;
import com.gc2026.portfolio.security.JwtUtil;
import com.gc2026.portfolio.security.TokenBlacklist;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklist tokenBlacklist;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("Email is already registered");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ValidationException("Username is already taken");
        }

        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.STANDARD)
                .isActive(true)
                .build();

        user = userRepository.save(user);

        // Seed default system categories for the new user
        seedDefaultCategories(user.getId());

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .user(toUserResponse(user))
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!user.getIsActive()) {
            throw new ValidationException("Account is deactivated");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .user(toUserResponse(user))
                .build();
    }

    public void logout(String token) {
        tokenBlacklist.blacklist(token);
    }

    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toUserResponse(user);
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private void seedDefaultCategories(Long userId) {
        List<Category> defaults = List.of(
                buildSystemCategory(userId, "Salaire", "#22C55E", CategoryType.REVENU),
                buildSystemCategory(userId, "Freelance", "#06B6D4", CategoryType.REVENU),
                buildSystemCategory(userId, "Alimentation", "#EF4444", CategoryType.DEPENSE),
                buildSystemCategory(userId, "Transport", "#F59E0B", CategoryType.DEPENSE),
                buildSystemCategory(userId, "Logement", "#3B82F6", CategoryType.DEPENSE),
                buildSystemCategory(userId, "Loisirs", "#8B5CF6", CategoryType.DEPENSE),
                buildSystemCategory(userId, "Santé", "#10B981", CategoryType.DEPENSE),
                buildSystemCategory(userId, "Autre", "#6B7280", CategoryType.BOTH)
        );
        categoryRepository.saveAll(defaults);
    }

    private Category buildSystemCategory(Long userId, String name, String color, CategoryType type) {
        return Category.builder()
                .userId(userId)
                .name(name)
                .color(color)
                .type(type)
                .isSystem(true)
                .build();
    }
}
