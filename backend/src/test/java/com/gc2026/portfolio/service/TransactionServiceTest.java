package com.gc2026.portfolio.service;

import com.gc2026.portfolio.domain.entity.Category;
import com.gc2026.portfolio.domain.enums.CategoryType;
import com.gc2026.portfolio.domain.enums.TransactionType;
import com.gc2026.portfolio.domain.exception.ResourceNotFoundException;
import com.gc2026.portfolio.dto.request.CreateTransactionRequest;
import com.gc2026.portfolio.repository.CategoryRepository;
import com.gc2026.portfolio.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void create_whenCategoryNotFoundForUser_shouldThrowResourceNotFound() {
        // Arrange
        Long userId = 1L;
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setCategoryId(999L);

        when(categoryRepository.findByIdAndUserIdOrSystem(999L, userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> transactionService.create(userId, request));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void create_whenCategoryIsSystem_shouldSucceed() {
        // Arrange
        Long userId = 1L;
        Long categoryId = 10L;
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setCategoryId(categoryId);
        request.setTitle("Test");
        request.setAmount(100L);
        request.setType(TransactionType.DEPENSE);
        request.setTxDate(LocalDate.now());

        Category systemCategory = Category.builder()
                .id(categoryId)
                .isSystem(true)
                .userId(0L)
                .name("System")
                .type(CategoryType.DEPENSE)
                .build();

        when(categoryRepository.findByIdAndUserIdOrSystem(categoryId, userId)).thenReturn(Optional.of(systemCategory));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        // Act
        transactionService.create(userId, request);

        // Assert
        verify(transactionRepository, times(1)).save(any());
    }
}
