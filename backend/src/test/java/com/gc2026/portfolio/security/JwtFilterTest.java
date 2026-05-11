package com.gc2026.portfolio.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenBlacklist tokenBlacklist;

    @InjectMocks
    private JwtFilter jwtFilter;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // We use StandaloneMockMvcBuilder to add the filter and a stub controller
        mockMvc = MockMvcBuilders.standaloneSetup(new StubController())
                .addFilters(jwtFilter)
                .build();

        // Clear security context before each test
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("1. Should proceed without authentication when no cookie is present")
    void filter_whenNoCookiePresent_shouldNotSetAuthenticationAndProceed() throws Exception {
        // Arrange & Act
        mockMvc.perform(get("/api/v1/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("userId=null"));

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("2. Should set security context and attributes when valid token in cookie")
    void filter_whenValidTokenInCookie_shouldSetSecurityContextAndAttributes() throws Exception {
        // Arrange
        String token = "valid.jwt";
        String email = "test@folio.io";
        Long userId = 1L;
        String role = "STANDARD";

        when(tokenBlacklist.isBlacklisted(token)).thenReturn(false);
        when(jwtUtil.isTokenValid(token)).thenReturn(true);
        when(jwtUtil.extractEmail(token)).thenReturn(email);
        when(jwtUtil.extractUserId(token)).thenReturn(userId);
        when(jwtUtil.extractRole(token)).thenReturn(role);

        // Act
        mockMvc.perform(get("/api/v1/test")
                .cookie(new Cookie("auth_token", token)))
                .andExpect(status().isOk())
                .andExpect(content().string("userId=1"));

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo(email);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_STANDARD");
    }

    @Test
    @DisplayName("3. Should not set authentication when token is blacklisted")
    void filter_whenTokenIsBlacklisted_shouldNotSetAuthentication() throws Exception {
        // Arrange
        String token = "blacklisted.jwt";
        when(tokenBlacklist.isBlacklisted(token)).thenReturn(true);

        // Act
        mockMvc.perform(get("/api/v1/test")
                .cookie(new Cookie("auth_token", token)))
                .andExpect(status().isOk())
                .andExpect(content().string("userId=null"));

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtUtil, never()).isTokenValid(anyString());
    }

    @Test
    @DisplayName("4. Should not set authentication when token is invalid")
    void filter_whenTokenIsInvalid_shouldNotSetAuthentication() throws Exception {
        // Arrange
        String token = "invalid.jwt";
        when(tokenBlacklist.isBlacklisted(token)).thenReturn(false);
        when(jwtUtil.isTokenValid(token)).thenReturn(false);

        // Act
        mockMvc.perform(get("/api/v1/test")
                .cookie(new Cookie("auth_token", token)))
                .andExpect(status().isOk())
                .andExpect(content().string("userId=null"));

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("5. Should not set authentication when token is expired")
    void filter_whenTokenIsExpired_shouldNotSetAuthentication() throws Exception {
        // Arrange
        String token = "expired.jwt";
        when(tokenBlacklist.isBlacklisted(token)).thenReturn(false);
        when(jwtUtil.isTokenValid(token)).thenReturn(false); // expired token results in false

        // Act
        mockMvc.perform(get("/api/v1/test")
                .cookie(new Cookie("auth_token", token)))
                .andExpect(status().isOk())
                .andExpect(content().string("userId=null"));

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("6. Should ignore Authorization header and only read cookies")
    void filter_shouldNotReadAuthorizationHeader_onlyReadCookies() throws Exception {
        // Arrange
        String token = "valid.jwt";
        // Note: we do NOT mock jwtUtil or tokenBlacklist because the filter shouldn't
        // even reach them
        // if it doesn't find the cookie.

        // Act
        mockMvc.perform(get("/api/v1/test")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("userId=null"));

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtUtil, tokenBlacklist);
    }

    @RestController
    static class StubController {
        @GetMapping("/api/v1/test")
        public ResponseEntity<String> test(HttpServletRequest req) {
            Long userId = (Long) req.getAttribute("userId");
            return ResponseEntity.ok("userId=" + userId);
        }
    }
}
