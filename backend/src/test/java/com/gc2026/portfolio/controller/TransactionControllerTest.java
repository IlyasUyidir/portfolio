package com.gc2026.portfolio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gc2026.portfolio.domain.enums.TransactionType;
import com.gc2026.portfolio.domain.exception.ResourceNotFoundException;
import com.gc2026.portfolio.dto.request.CreateTransactionRequest;
import com.gc2026.portfolio.dto.request.UpdateTransactionRequest;
import com.gc2026.portfolio.dto.response.CategoryResponse;
import com.gc2026.portfolio.dto.response.PaginatedResponse;
import com.gc2026.portfolio.dto.response.TransactionResponse;
import com.gc2026.portfolio.security.JwtFilter;
import com.gc2026.portfolio.security.RateLimitFilter;
import com.gc2026.portfolio.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private JwtFilter jwtFilter;

    @MockBean
    private RateLimitFilter rateLimitFilter;

    private TransactionResponse txResponse;
    private CategoryResponse catResponse;

    @BeforeEach
    void setUp() {
        catResponse = CategoryResponse.builder()
                .id(1L)
                .name("Alimentation")
                .color("#EF4444")
                .type("DEPENSE")
                .isSystem(true)
                .build();

        txResponse = TransactionResponse.builder()
                .id(100L)
                .title("Courses")
                .amount(5000L)
                .type("DEPENSE")
                .category(catResponse)
                .txDate(LocalDate.of(2026, 5, 10))
                .build();
    }

    // --- POST /api/v1/transactions ---

    @Test
    @DisplayName("1. create_whenValidRequest_shouldReturn201")
    void create_whenValidRequest_shouldReturn201() throws Exception {
        // Arrange
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .title("Courses")
                .amount(5000L)
                .type(TransactionType.DEPENSE)
                .categoryId(1L)
                .txDate(LocalDate.of(2026, 5, 10))
                .build();

        when(transactionService.create(eq(1L), any(CreateTransactionRequest.class))).thenReturn(txResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/transactions")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.title").value("Courses"));
    }

    @Test
    @DisplayName("2. create_whenMissingTitle_shouldReturn400")
    void create_whenMissingTitle_shouldReturn400() throws Exception {
        // Arrange
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .amount(5000L)
                .type(TransactionType.DEPENSE)
                .categoryId(1L)
                .txDate(LocalDate.of(2026, 5, 10))
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/transactions")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("3. create_whenAmountIsZero_shouldReturn400")
    void create_whenAmountIsZero_shouldReturn400() throws Exception {
        // Arrange
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .title("Courses")
                .amount(0L)
                .type(TransactionType.DEPENSE)
                .categoryId(1L)
                .txDate(LocalDate.of(2026, 5, 10))
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/transactions")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("4. create_whenAmountIsNegative_shouldReturn400")
    void create_whenAmountIsNegative_shouldReturn400() throws Exception {
        // Arrange
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .title("Courses")
                .amount(-100L)
                .type(TransactionType.DEPENSE)
                .categoryId(1L)
                .txDate(LocalDate.of(2026, 5, 10))
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/transactions")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("S2-6. create_whenAmountIsOneCentime_shouldReturn201")
    void create_whenAmountIsOneCentime_shouldReturn201() throws Exception {
        // Arrange
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .title("Minor")
                .amount(1L) // 0.01 DH
                .type(TransactionType.DEPENSE)
                .categoryId(1L)
                .txDate(LocalDate.of(2026, 5, 10))
                .build();

        txResponse.setAmount(1L);
        txResponse.setTitle("Minor");

        when(transactionService.create(eq(1L), any(CreateTransactionRequest.class))).thenReturn(txResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/transactions")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(1L));
    }

    // --- GET /api/v1/transactions ---

    @Test
    @DisplayName("5. list_withNoParams_shouldReturn200WithPaginatedResponse")
    void list_withNoParams_shouldReturn200WithPaginatedResponse() throws Exception {
        // Arrange
        PaginatedResponse<TransactionResponse> paginatedResponse = PaginatedResponse.<TransactionResponse>builder()
                .content(List.of(txResponse))
                .totalElements(1)
                .totalPages(1)
                .number(0)
                .size(20)
                .build();

        when(transactionService.list(eq(1L), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/transactions")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(100L))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("6. list_shouldClampSizeToMax100")
    void list_shouldClampSizeToMax100() throws Exception {
        // Arrange
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        
        when(transactionService.list(eq(1L), any(), any(), any(), any(), any(), pageableCaptor.capture()))
                .thenReturn(PaginatedResponse.<TransactionResponse>builder().content(List.of()).build());

        // Act
        mockMvc.perform(get("/api/v1/transactions")
                        .param("size", "9999")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk());

        // Assert
        assertThat(pageableCaptor.getValue().getPageSize()).isLessThanOrEqualTo(100);
    }

    @Test
    @DisplayName("7. list_shouldDefaultPageSizeTo20")
    void list_shouldDefaultPageSizeTo20() throws Exception {
        // Arrange
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        
        when(transactionService.list(eq(1L), any(), any(), any(), any(), any(), pageableCaptor.capture()))
                .thenReturn(PaginatedResponse.<TransactionResponse>builder().content(List.of()).build());

        // Act
        mockMvc.perform(get("/api/v1/transactions")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk());

        // Assert
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    // --- GET /api/v1/transactions/{id} ---

    @Test
    @DisplayName("8. getById_whenFound_shouldReturn200")
    void getById_whenFound_shouldReturn200() throws Exception {
        // Arrange
        when(transactionService.getById(1L, 100L)).thenReturn(txResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/transactions/100")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L));
    }

    @Test
    @DisplayName("9. getById_whenNotFound_shouldReturn404")
    void getById_whenNotFound_shouldReturn404() throws Exception {
        // Arrange
        when(transactionService.getById(any(), any())).thenThrow(new ResourceNotFoundException("Transaction not found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/transactions/999")
                        .requestAttr("userId", 1L))
                .andExpect(status().isNotFound());
    }

    // --- PUT /api/v1/transactions/{id} ---

    @Test
    @DisplayName("10. update_whenValidRequest_shouldReturn200")
    void update_whenValidRequest_shouldReturn200() throws Exception {
        // Arrange
        UpdateTransactionRequest request = UpdateTransactionRequest.builder()
                .title("Updated Courses")
                .amount(6000L)
                .type(TransactionType.DEPENSE)
                .categoryId(1L)
                .txDate(LocalDate.of(2026, 5, 10))
                .build();

        txResponse.setTitle("Updated Courses");
        txResponse.setAmount(6000L);

        when(transactionService.update(eq(1L), eq(100L), any(UpdateTransactionRequest.class))).thenReturn(txResponse);

        // Act & Assert
        mockMvc.perform(put("/api/v1/transactions/100")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Courses"))
                .andExpect(jsonPath("$.amount").value(6000L));
    }

    @Test
    @DisplayName("11. update_whenBodyIsEmpty_shouldReturn400")
    void update_whenBodyIsEmpty_shouldReturn400() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/v1/transactions/100")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // --- DELETE /api/v1/transactions/{id} ---

    @Test
    @DisplayName("12. delete_whenFound_shouldReturn200WithMessage")
    void delete_whenFound_shouldReturn200WithMessage() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/transactions/100")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transaction deleted"));

        verify(transactionService).delete(1L, 100L);
    }

    @Test
    @DisplayName("13. delete_whenNotFound_shouldReturn404")
    void delete_whenNotFound_shouldReturn404() throws Exception {
        // Arrange
        when(transactionService.getById(any(), any())).thenThrow(new ResourceNotFoundException("Transaction not found"));
        // Note: Controller calls transactionService.delete directly. If delete throws RNF, it should return 404.
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Transaction not found"))
                .when(transactionService).delete(any(), any());

        // Act & Assert
        mockMvc.perform(delete("/api/v1/transactions/999")
                        .requestAttr("userId", 1L))
                .andExpect(status().isNotFound());
    }
}
