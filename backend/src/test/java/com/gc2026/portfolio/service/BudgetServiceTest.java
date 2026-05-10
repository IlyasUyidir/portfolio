package com.gc2026.portfolio.service;

import com.gc2026.portfolio.domain.entity.Budget;
import com.gc2026.portfolio.domain.entity.Category;
import com.gc2026.portfolio.domain.enums.CategoryType;
import com.gc2026.portfolio.domain.exception.ResourceNotFoundException;
import com.gc2026.portfolio.dto.request.CreateBudgetRequest;
import com.gc2026.portfolio.repository.BudgetRepository;
import com.gc2026.portfolio.repository.CategoryRepository;
import com.gc2026.portfolio.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private BudgetService budgetService;

    @Test
    void createOrUpdate_whenCategoryNotFoundForUser_shouldThrowResourceNotFound() {
        // Arrange
        Long userId = 1L;
        Long categoryId = 999L;
        CreateBudgetRequest request = new CreateBudgetRequest();
        request.setCategoryId(categoryId);
        request.setLimitAmount(1000L);
        request.setBudgetYear(2026);
        request.setBudgetMonth(5);

        // This simulates the behavior where findByIdAndUserIdOrSystem returns empty 
        // if category doesn't belong to user and is not system
        when(categoryRepository.findByIdAndUserIdOrSystem(categoryId, userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> budgetService.createOrUpdate(userId, request));
        verify(budgetRepository, never()).save(any());
    }

    @Test
    void createOrUpdate_whenCategoryIsSystem_shouldSucceed() {
        // Arrange
        Long userId = 1L;
        Long categoryId = 10L;
        CreateBudgetRequest request = new CreateBudgetRequest();
        request.setCategoryId(categoryId);
        request.setLimitAmount(1000L);
        request.setBudgetYear(2026);
        request.setBudgetMonth(5);

        Category systemCategory = Category.builder()
                .id(categoryId)
                .isSystem(true)
                .userId(0L)
                .name("System")
                .type(CategoryType.DEPENSE)
                .build();

        when(categoryRepository.findByIdAndUserIdOrSystem(categoryId, userId))
                .thenReturn(Optional.of(systemCategory));
        when(budgetRepository.findByUserIdAndCategoryIdAndBudgetYearAndBudgetMonth(any(), any(), anyInt(), anyInt()))
                .thenReturn(Optional.empty());
        when(budgetRepository.save(any(Budget.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        budgetService.createOrUpdate(userId, request);

        // Assert
        verify(budgetRepository, times(1)).save(any(Budget.class));
    }
}
