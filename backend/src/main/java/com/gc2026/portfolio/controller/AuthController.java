package com.gc2026.portfolio.controller;

import com.gc2026.portfolio.dto.request.LoginRequest;
import com.gc2026.portfolio.dto.request.RegisterRequest;
import com.gc2026.portfolio.dto.response.AuthResponse;
import com.gc2026.portfolio.dto.response.UserResponse;
import com.gc2026.portfolio.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        AuthResponse auth = authService.register(request);
        setTokenCookie(response, auth.getToken());
        return ResponseEntity.ok(auth.getUser());
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResponse auth = authService.login(request);
        setTokenCookie(response, auth.getToken());
        return ResponseEntity.ok(auth.getUser());
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = extractTokenFromCookie(request);
        if (token != null) {
            authService.logout(token);
        }
        
        // Clear cookie
        Cookie cookie = new Cookie("auth_token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        UserResponse response = authService.getCurrentUser(userId);
        return ResponseEntity.ok(response);
    }

    private void setTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("auth_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // Should be true in production (HTTPS)
        cookie.setPath("/");
        cookie.setMaxAge(86400); // 1 day
        // cookie.setAttribute("SameSite", "Strict"); // Spring Boot 3+ Cookie supports this better via headers or specialized classes
        response.addCookie(cookie);
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if ("auth_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
