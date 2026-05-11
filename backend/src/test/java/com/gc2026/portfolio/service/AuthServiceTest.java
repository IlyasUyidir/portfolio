package com.gc2026.portfolio.service;

import com.gc2026.portfolio.domain.entity.Category;
import com.gc2026.portfolio.domain.entity.User;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenBlacklist tokenBlacklist;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@folio.io")
                .username("testuser")
                .passwordHash("$2a$10$hashedpw")
                .role(UserRole.STANDARD)
                .isActive(true)
                .build();

        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@folio.io");
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("password123");
    }

    // --- REGISTER ---

    @Test
    void register_whenEmailAlreadyExists_shouldThrowValidationException() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(ValidationException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_whenUsernameAlreadyExists_shouldThrowValidationException() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(ValidationException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_whenValidRequest_shouldSaveUserAndReturnToken() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPw");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(anyLong(), anyString(), any(UserRole.class))).thenReturn("jwt.token.string");

        // Act
        AuthResponse result = authService.register(registerRequest);

        // Assert
        assertThat(result.getToken()).isEqualTo("jwt.token.string");
        assertThat(result.getUser().getEmail()).isEqualTo("test@folio.io");
        verify(userRepository).save(any(User.class));
        verify(categoryRepository).saveAll(anyList());
    }

    @Test
    @SuppressWarnings("unchecked")
    void register_whenValidRequest_shouldSeedExactly8DefaultCategories() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPw");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        authService.register(registerRequest);

        // Assert
        ArgumentCaptor<List<Category>> captor = ArgumentCaptor.forClass(List.class);
        verify(categoryRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(8);
        assertThat(captor.getValue().stream().allMatch(Category::getIsSystem)).isTrue();
    }

    @Test
    void register_shouldEncodePasswordBeforeSaving() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPw");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        authService.register(registerRequest);

        // Assert
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("encodedPw");
        assertThat(captor.getValue().getPasswordHash()).isNotEqualTo("password123");
    }

    // --- LOGIN ---

    @Test
    void login_whenEmailNotFound_shouldThrowBadCredentialsException() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest("unknown@folio.io", "password123");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
    }

    @Test
    void login_whenPasswordDoesNotMatch_shouldThrowBadCredentialsException() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest("test@folio.io", "wrongpassword");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", testUser.getPasswordHash())).thenReturn(false);

        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
    }

    @Test
    void login_whenAccountIsInactive_shouldThrowValidationException() {
        // Arrange
        testUser.setIsActive(false);
        LoginRequest loginRequest = new LoginRequest("test@folio.io", "password123");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(true);

        // Act & Assert
        assertThrows(ValidationException.class, () -> authService.login(loginRequest));
    }

    @Test
    void login_whenValidCredentials_shouldReturnToken() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest("test@folio.io", "password123");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(true);
        when(jwtUtil.generateToken(1L, "test@folio.io", UserRole.STANDARD)).thenReturn("valid.jwt");

        // Act
        AuthResponse result = authService.login(loginRequest);

        // Assert
        assertThat(result.getToken()).isEqualTo("valid.jwt");
        verify(jwtUtil).generateToken(1L, "test@folio.io", UserRole.STANDARD);
    }

    // --- LOGOUT ---

    @Test
    void logout_shouldBlacklistToken() {
        // Act
        authService.logout("some.token");

        // Assert
        verify(tokenBlacklist).blacklist("some.token");
    }

    // --- GET CURRENT USER ---

    @Test
    void getCurrentUser_whenUserExists_shouldReturnUserResponse() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        UserResponse result = authService.getCurrentUser(1L);

        // Assert
        assertThat(result.getEmail()).isEqualTo(testUser.getEmail());
        assertThat(result.getUsername()).isEqualTo(testUser.getUsername());
    }

    @Test
    void getCurrentUser_whenUserNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> authService.getCurrentUser(999L));
    }

    // --- EDGE CASES ---

    @Test
    void register_passwordShouldNeverAppearInResponse() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPw");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        AuthResponse result = authService.register(registerRequest);

        // Assert
        assertThat(result.getUser()).isNotNull();
        // UserResponse doesn't have passwordHash field, but we check the object returned
        // to ensure it matches the UserResponse structure which excludes password.
        assertThat(result.getUser().getClass().getDeclaredFields())
                .extracting("name")
                .doesNotContain("password", "passwordHash");
    }
}
