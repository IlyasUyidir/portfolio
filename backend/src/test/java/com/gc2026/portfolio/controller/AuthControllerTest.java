package com.gc2026.portfolio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gc2026.portfolio.domain.exception.ValidationException;
import com.gc2026.portfolio.dto.request.LoginRequest;
import com.gc2026.portfolio.dto.request.RegisterRequest;
import com.gc2026.portfolio.dto.response.AuthResponse;
import com.gc2026.portfolio.dto.response.UserResponse;
import com.gc2026.portfolio.security.JwtFilter;
import com.gc2026.portfolio.security.RateLimitFilter;
import com.gc2026.portfolio.service.AuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtFilter jwtFilter; // exclude from filter chain

    @MockitoBean
    private RateLimitFilter rateLimitFilter;

    private UserResponse userResponse;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        userResponse = UserResponse.builder()
                .id(1L)
                .email("test@folio.io")
                .username("testuser")
                .role("STANDARD")
                .build();

        authResponse = AuthResponse.builder()
                .token("jwt.token")
                .user(userResponse)
                .build();
    }

    // --- POST /api/v1/auth/register ---

    @Test
    @DisplayName("1. register_whenValidRequest_shouldReturn200AndSetCookie")
    void register_whenValidRequest_shouldReturn200AndSetCookie() throws Exception {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .email("test@folio.io")
                .username("testuser")
                .password("password123")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(cookie().exists("auth_token"))
                .andExpect(cookie().httpOnly("auth_token", true))
                .andExpect(cookie().secure("auth_token", true))
                .andExpect(cookie().path("auth_token", "/"))
                .andExpect(cookie().maxAge("auth_token", 86400));
    }

    @Test
    @DisplayName("2. register_shouldNotReturnTokenInResponseBody")
    void register_shouldNotReturnTokenInResponseBody() throws Exception {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .email("test@folio.io")
                .username("testuser")
                .password("password123")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(content().string(not(containsString("jwt.token"))));
    }

    @Test
    @DisplayName("3. register_whenInvalidEmail_shouldReturn400")
    void register_whenInvalidEmail_shouldReturn400() throws Exception {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .email("not-an-email")
                .username("testuser")
                .password("password123")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("4. register_whenMissingFields_shouldReturn400")
    void register_whenMissingFields_shouldReturn400() throws Exception {
        // Arrange
        String emptyBody = "{}";

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("5. register_whenPasswordTooShort_shouldReturn400")
    void register_whenPasswordTooShort_shouldReturn400() throws Exception {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .email("test@folio.io")
                .username("testuser")
                .password("12345") // less than 6 chars
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("6. register_whenEmailAlreadyExists_shouldReturn400")
    void register_whenEmailAlreadyExists_shouldReturn400() throws Exception {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .email("test@folio.io")
                .username("testuser")
                .password("password123")
                .build();

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new ValidationException("Email is already registered"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.error").value("Email is already registered"));
    }

    @Test
    @DisplayName("S2-1. register_withUsernameExactly3Chars_shouldSucceed")
    void register_withUsernameExactly3Chars_shouldSucceed() throws Exception {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .email("test3@folio.io")
                .username("abc") // exactly 3
                .password("password123")
                .build();
        when(authService.register(any())).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("S2-2. register_withUsername2Chars_shouldReturn400")
    void register_withUsername2Chars_shouldReturn400() throws Exception {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .email("test2@folio.io")
                .username("ab") // too short
                .password("password123")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("S2-3. register_withPasswordExactly6Chars_shouldSucceed")
    void register_withPasswordExactly6Chars_shouldSucceed() throws Exception {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .email("test6@folio.io")
                .username("testuser")
                .password("123456") // exactly 6
                .build();
        when(authService.register(any())).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("S2-4. register_withUsernameExactly50Chars_shouldSucceed")
    void register_withUsernameExactly50Chars_shouldSucceed() throws Exception {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .email("test50@folio.io")
                .username("a".repeat(50)) // exactly 50
                .password("password123")
                .build();
        when(authService.register(any())).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("S2-5. register_withUsername51Chars_shouldReturn400")
    void register_withUsername51Chars_shouldReturn400() throws Exception {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .email("test51@folio.io")
                .username("a".repeat(51)) // too long
                .password("password123")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // --- POST /api/v1/auth/login ---

    @Test
    @DisplayName("7. login_whenValidCredentials_shouldReturn200AndSetCookie")
    void login_whenValidCredentials_shouldReturn200AndSetCookie() throws Exception {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .email("test@folio.io")
                .password("password123")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("auth_token"))
                .andExpect(cookie().httpOnly("auth_token", true))
                .andExpect(cookie().secure("auth_token", true))
                .andExpect(cookie().path("auth_token", "/"))
                .andExpect(cookie().maxAge("auth_token", 86400));
    }

    @Test
    @DisplayName("8. login_whenInvalidCredentials_shouldReturn401")
    void login_whenInvalidCredentials_shouldReturn401() throws Exception {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .email("test@folio.io")
                .password("wrongpassword")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    @Test
    @DisplayName("9. login_whenInvalidEmailFormat_shouldReturn400")
    void login_whenInvalidEmailFormat_shouldReturn400() throws Exception {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .email("notvalid")
                .password("password123")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("9b. login_shouldSetSameSiteLaxCookie")
    void login_shouldSetSameSiteLaxCookie() throws Exception {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .email("test@folio.io")
                .password("password123")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")));
    }

    // --- POST /api/v1/auth/logout ---

    @Test
    @DisplayName("10. logout_shouldClearCookieAndReturn200")
    void logout_shouldClearCookieAndReturn200() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new Cookie("auth_token", "some.token")))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("auth_token", 0))
                .andExpect(cookie().value("auth_token", ""));
    }

    @Test
    @DisplayName("11. logout_whenNoCookiePresent_shouldStillReturn200")
    void logout_whenNoCookiePresent_shouldStillReturn200() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk());
    }

    // --- GET /api/v1/auth/me ---

    @Test
    @DisplayName("12. me_shouldReturn200WithUserProfile")
    void me_shouldReturn200WithUserProfile() throws Exception {
        // Arrange
        when(authService.getCurrentUser(1L)).thenReturn(userResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/me")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@folio.io"));
    }
}
