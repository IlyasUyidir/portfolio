package com.gc2026.portfolio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gc2026.portfolio.domain.enums.CategoryType;
import com.gc2026.portfolio.domain.exception.ValidationException;
import com.gc2026.portfolio.dto.request.CreateCategoryRequest;
import com.gc2026.portfolio.dto.request.UpdateCategoryRequest;
import com.gc2026.portfolio.dto.response.CategoryResponse;
import com.gc2026.portfolio.security.JwtFilter;
import com.gc2026.portfolio.security.RateLimitFilter;
import com.gc2026.portfolio.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import com.gc2026.portfolio.config.SecurityConfig;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@Import(SecurityConfig.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private RateLimitFilter rateLimitFilter;

    private CategoryResponse catResponse;

    @BeforeEach
    void setUp() throws ServletException, IOException {
        // Mock filters to be transparent
        doAnswer(invocation -> {
            ((FilterChain) invocation.getArgument(2)).doFilter(
                    invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());

        doAnswer(invocation -> {
            ((FilterChain) invocation.getArgument(2)).doFilter(
                    invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(rateLimitFilter).doFilter(any(), any(), any());

        catResponse = CategoryResponse.builder()
                .id(5L)
                .name("Voyage")
                .color("#FF0000")
                .type("DEPENSE")
                .isSystem(false)
                .build();
    }

    // --- GET /api/v1/categories ---

    @Test
    @WithMockUser
    @DisplayName("1. list_shouldReturn200WithCategoryList")
    void list_shouldReturn200WithCategoryList() throws Exception {
        // Arrange
        when(categoryService.list(any())).thenReturn(List.of(catResponse));

        // Act & Assert
        mockMvc.perform(get("/api/v1/categories")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Voyage"));
    }

    // --- POST /api/v1/categories ---

    @Test
    @WithMockUser
    @DisplayName("2. create_whenValidRequest_shouldReturn201")
    void create_whenValidRequest_shouldReturn201() throws Exception {
        // Arrange
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Voyage")
                .type(CategoryType.DEPENSE)
                .color("#FF0000")
                .build();

        when(categoryService.create(eq(1L), any(), any(CreateCategoryRequest.class))).thenReturn(catResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/categories")
                        .requestAttr("userId", 1L)
                        .requestAttr("userRole", "STANDARD")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.name").value("Voyage"));
    }

    @Test
    @WithMockUser
    @DisplayName("3. create_whenInvalidColorFormat_shouldReturn400")
    void create_whenInvalidColorFormat_shouldReturn400() throws Exception {
        // Arrange
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Voyage")
                .type(CategoryType.DEPENSE)
                .color("red") // invalid hex
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/categories")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("4. create_whenValidColorHex_shouldReturn201")
    void create_whenValidColorHex_shouldReturn201() throws Exception {
        // Arrange
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Voyage")
                .type(CategoryType.DEPENSE)
                .color("#FF0000") // valid hex
                .build();

        when(categoryService.create(eq(1L), any(), any(CreateCategoryRequest.class))).thenReturn(catResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/categories")
                        .requestAttr("userId", 1L)
                        .requestAttr("userRole", "STANDARD")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    @DisplayName("5. create_whenNameIsBlank_shouldReturn400")
    void create_whenNameIsBlank_shouldReturn400() throws Exception {
        // Arrange
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("") // blank
                .type(CategoryType.DEPENSE)
                .color("#FF0000")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/categories")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("S2-7. create_withNameExactly100Chars_shouldReturn201")
    void create_withNameExactly100Chars_shouldReturn201() throws Exception {
        // Arrange
        String longName = "A".repeat(100);
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name(longName)
                .type(CategoryType.DEPENSE)
                .color("#FF0000")
                .build();

        catResponse.setName(longName);
        when(categoryService.create(eq(1L), any(), any(CreateCategoryRequest.class))).thenReturn(catResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/categories")
                        .requestAttr("userId", 1L)
                        .requestAttr("userRole", "STANDARD")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    @DisplayName("S2-8. create_withName101Chars_shouldReturn400")
    void create_withName101Chars_shouldReturn400() throws Exception {
        // Arrange
        String tooLongName = "A".repeat(101);
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name(tooLongName)
                .type(CategoryType.DEPENSE)
                .color("#FF0000")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/categories")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("S2-9. create_withShortHex_shouldReturn400")
    void create_withShortHex_shouldReturn400() throws Exception {
        // Arrange
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Voyage")
                .type(CategoryType.DEPENSE)
                .color("#FFF") // shorthand not allowed by regex
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/categories")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("S2-10. create_withoutHash_shouldReturn400")
    void create_withoutHash_shouldReturn400() throws Exception {
        // Arrange
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Voyage")
                .type(CategoryType.DEPENSE)
                .color("FF0000") // missing #
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/categories")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // --- PUT /api/v1/categories/{id} ---

    @Test
    @WithMockUser
    @DisplayName("6. update_whenValidRequest_shouldReturn200")
    void update_whenValidRequest_shouldReturn200() throws Exception {
        // Arrange
        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .name("Voyage Pro")
                .type(CategoryType.DEPENSE)
                .color("#FF0000")
                .build();

        catResponse.setName("Voyage Pro");

        when(categoryService.update(eq(1L), eq(5L), any(UpdateCategoryRequest.class))).thenReturn(catResponse);

        // Act & Assert
        mockMvc.perform(put("/api/v1/categories/5")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Voyage Pro"));
    }

    @Test
    @WithMockUser
    // I-5: ValidationException (business-rule violation) → 409 Conflict, not 400.
    @DisplayName("7. update_whenSystemCategory_shouldReturn409")
    void update_whenSystemCategory_shouldReturn409() throws Exception {
        // Arrange
        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .name("System")
                .type(CategoryType.DEPENSE)
                .color("#FF0000")
                .build();

        when(categoryService.update(any(), any(), any())).thenThrow(new ValidationException("System categories cannot be modified"));

        // Act & Assert
        mockMvc.perform(put("/api/v1/categories/5")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());  // 409 — was 400 before I-5 fix
    }

    // --- DELETE /api/v1/categories/{id} ---

    @Test
    @WithMockUser
    @DisplayName("8. delete_whenFound_shouldReturn200WithMessage")
    void delete_whenFound_shouldReturn200WithMessage() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/categories/5")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Category deleted"));
    }

    @Test
    @WithMockUser
    // I-5: ValidationException (business-rule violation) → 409 Conflict, not 400.
    @DisplayName("9. delete_whenCategoryHasTransactions_shouldReturn409")
    void delete_whenCategoryHasTransactions_shouldReturn409() throws Exception {
        // Arrange
        org.mockito.Mockito.doThrow(new ValidationException("Cannot delete category with existing transactions"))
                .when(categoryService).delete(any(), any());

        // Act & Assert
        mockMvc.perform(delete("/api/v1/categories/5")
                        .requestAttr("userId", 1L))
                .andExpect(status().isConflict());  // 409 — was 400 before I-5 fix
    }
}
