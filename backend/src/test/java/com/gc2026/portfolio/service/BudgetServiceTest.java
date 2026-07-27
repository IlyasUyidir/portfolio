package com.gc2026.portfolio.service;

import com.gc2026.portfolio.domain.entity.Budget;
import com.gc2026.portfolio.domain.entity.Category;
import com.gc2026.portfolio.domain.enums.CategoryType;
import com.gc2026.portfolio.domain.enums.TransactionType;
import com.gc2026.portfolio.domain.exception.ResourceNotFoundException;
import com.gc2026.portfolio.domain.exception.ValidationException;
import com.gc2026.portfolio.dto.request.CreateBudgetRequest;
import com.gc2026.portfolio.dto.response.BudgetProgressResponse;
import com.gc2026.portfolio.dto.response.BudgetResponse;
import com.gc2026.portfolio.repository.BudgetRepository;
import com.gc2026.portfolio.repository.CategoryRepository;
import com.gc2026.portfolio.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
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

    private Long userId;
    private Category cat;
    private Budget budget;
    private CreateBudgetRequest validRequest;

    @BeforeEach
    void setUp() {
        userId = 1L;
        cat = Category.builder()
                .id(10L)
                .userId(1L)
                .name("Alimentation")
                .type(CategoryType.DEPENSE)
                .isSystem(false)
                .color("#EF4444")
                .build();

        budget = Budget.builder()
                .id(1L)
                .userId(1L)
                .category(cat)
                .budgetYear(2026)
                .budgetMonth(5)
                .limitAmount(200000L)
                .alertThreshold(80)
                .build();

        validRequest = new CreateBudgetRequest();
        validRequest.setCategoryId(10L);
        validRequest.setBudgetYear(2026);
        validRequest.setBudgetMonth(5);
        validRequest.setLimitAmount(200000L);
        validRequest.setAlertThreshold(80);
    }

    // --- CREATE OR UPDATE ---

    @Test
    void createOrUpdate_whenCategoryNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        when(categoryRepository.findByIdAndUserIdOrSystem(anyLong(), anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> budgetService.createOrUpdate(userId, validRequest));
        verify(budgetRepository, never()).save(any());
    }

    @Test
    void createOrUpdate_whenLimitAmountIsZero_shouldThrowValidationException() {
        // Arrange
        validRequest.setLimitAmount(0L);

        // Act & Assert
        assertThrows(ValidationException.class, () -> budgetService.createOrUpdate(userId, validRequest));
    }

    @Test
    void createOrUpdate_whenLimitAmountIsNegative_shouldThrowValidationException() {
        // Arrange
        validRequest.setLimitAmount(-5000L);

        // Act & Assert
        assertThrows(ValidationException.class, () -> budgetService.createOrUpdate(userId, validRequest));
    }

    @Test
    void createOrUpdate_withBudgetMonth1_shouldSucceed() {
        // Arrange
        validRequest.setBudgetMonth(1);
        when(categoryRepository.findByIdAndUserIdOrSystem(anyLong(), anyLong())).thenReturn(Optional.of(cat));
        when(budgetRepository.findByUserIdAndCategoryIdAndBudgetYearAndBudgetMonth(anyLong(), anyLong(), anyInt(), anyInt()))
                .thenReturn(Optional.empty());
        when(budgetRepository.save(any(Budget.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        BudgetResponse response = budgetService.createOrUpdate(userId, validRequest);

        // Assert
        assertThat(response.getBudgetMonth()).isEqualTo(1);
    }

    @Test
    void createOrUpdate_withBudgetMonth12_shouldSucceed() {
        // Arrange
        validRequest.setBudgetMonth(12);
        when(categoryRepository.findByIdAndUserIdOrSystem(anyLong(), anyLong())).thenReturn(Optional.of(cat));
        when(budgetRepository.findByUserIdAndCategoryIdAndBudgetYearAndBudgetMonth(anyLong(), anyLong(), anyInt(), anyInt()))
                .thenReturn(Optional.empty());
        when(budgetRepository.save(any(Budget.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        BudgetResponse response = budgetService.createOrUpdate(userId, validRequest);

        // Assert
        assertThat(response.getBudgetMonth()).isEqualTo(12);
    }

    @Test
    void createOrUpdate_whenBudgetDoesNotExist_shouldCreateNew() {
        // Arrange
        when(categoryRepository.findByIdAndUserIdOrSystem(anyLong(), anyLong())).thenReturn(Optional.of(cat));
        when(budgetRepository.findByUserIdAndCategoryIdAndBudgetYearAndBudgetMonth(anyLong(), anyLong(), anyInt(), anyInt()))
                .thenReturn(Optional.empty());
        when(budgetRepository.save(any(Budget.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        BudgetResponse response = budgetService.createOrUpdate(userId, validRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getLimitAmount()).isEqualTo(validRequest.getLimitAmount());
        verify(budgetRepository).save(any(Budget.class));
    }

    @Test
    void createOrUpdate_whenBudgetAlreadyExists_shouldUpdateExisting() {
        // Arrange
        when(categoryRepository.findByIdAndUserIdOrSystem(anyLong(), anyLong())).thenReturn(Optional.of(cat));
        when(budgetRepository.findByUserIdAndCategoryIdAndBudgetYearAndBudgetMonth(anyLong(), anyLong(), anyInt(), anyInt()))
                .thenReturn(Optional.of(budget));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(i -> i.getArguments()[0]);

        validRequest.setLimitAmount(300000L);

        // Act
        BudgetResponse response = budgetService.createOrUpdate(userId, validRequest);

        // Assert
        assertThat(response.getLimitAmount()).isEqualTo(300000L);
        assertThat(budget.getLimitAmount()).isEqualTo(300000L);
        verify(budgetRepository).save(budget);
    }

    @Test
    void createOrUpdate_whenAlertThresholdIsNull_shouldDefaultTo80() {
        // Arrange
        validRequest.setAlertThreshold(null);
        when(categoryRepository.findByIdAndUserIdOrSystem(anyLong(), anyLong())).thenReturn(Optional.of(cat));
        when(budgetRepository.findByUserIdAndCategoryIdAndBudgetYearAndBudgetMonth(anyLong(), anyLong(), anyInt(), anyInt()))
                .thenReturn(Optional.empty());
        when(budgetRepository.save(any(Budget.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        budgetService.createOrUpdate(userId, validRequest);

        // Assert
        ArgumentCaptor<Budget> captor = ArgumentCaptor.forClass(Budget.class);
        verify(budgetRepository).save(captor.capture());
        assertThat(captor.getValue().getAlertThreshold()).isEqualTo(80);
    }

    // --- GET BUDGETS BY MONTH ---

    @Test
    void getBudgetsByMonth_shouldReturnProgressForAllBudgetsInMonth() {
        // Arrange
        YearMonth month = YearMonth.of(2026, 5);
        when(budgetRepository.findByUserIdAndBudgetYearAndBudgetMonth(userId, 2026, 5))
                .thenReturn(List.of(budget, budget, budget));
        when(transactionRepository.calculateSpentAmountForCategoryAndMonth(anyLong(), anyLong(), any(), any(), any()))
                .thenReturn(50000L);

        // Act
        List<BudgetProgressResponse> results = budgetService.getBudgetsByMonth(userId, month);

        // Assert
        assertThat(results).hasSize(3);
        verify(transactionRepository, times(3)).calculateSpentAmountForCategoryAndMonth(anyLong(), anyLong(), any(), any(), any());
    }

    // --- GET PROGRESS ---

    @Test
    void getProgress_whenBudgetNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        when(budgetRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> budgetService.getProgress(userId, 1L));
    }

    @Test
    void getProgress_whenSpentIsUnderThreshold_shouldReturnNormalStatus() {
        // Arrange
        budget.setLimitAmount(200000L);
        budget.setAlertThreshold(80);
        when(budgetRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(budget));
        when(transactionRepository.calculateSpentAmountForCategoryAndMonth(anyLong(), anyLong(), any(), any(), any()))
                .thenReturn(100000L); // 50%

        // Act
        BudgetProgressResponse result = budgetService.getProgress(userId, 1L);

        // Assert
        assertThat(result.getAlertStatus()).isEqualTo("NORMAL");
    }

    @Test
    void getProgress_whenSpentExceedsThreshold_shouldReturnWarningStatus() {
        // Arrange
        budget.setLimitAmount(200000L);
        budget.setAlertThreshold(80);
        when(budgetRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(budget));
        when(transactionRepository.calculateSpentAmountForCategoryAndMonth(anyLong(), anyLong(), any(), any(), any()))
                .thenReturn(170000L); // 85%

        // Act
        BudgetProgressResponse result = budgetService.getProgress(userId, 1L);

        // Assert
        assertThat(result.getAlertStatus()).isEqualTo("WARNING");
    }

    @Test
    void getProgress_whenSpentExceedsLimit_shouldReturnCriticalStatus() {
        // Arrange
        budget.setLimitAmount(200000L);
        when(budgetRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(budget));
        when(transactionRepository.calculateSpentAmountForCategoryAndMonth(anyLong(), anyLong(), any(), any(), any()))
                .thenReturn(250000L); // 125%

        // Act
        BudgetProgressResponse result = budgetService.getProgress(userId, 1L);

        // Assert
        assertThat(result.getAlertStatus()).isEqualTo("CRITICAL");
        assertThat(result.getRemainingAmount()).isEqualTo(0L);
    }

    @Test
    void getProgress_shouldCalculateSpentPercentageCorrectly() {
        // Arrange
        budget.setLimitAmount(100000L);
        when(budgetRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(budget));
        when(transactionRepository.calculateSpentAmountForCategoryAndMonth(anyLong(), anyLong(), any(), any(), any()))
                .thenReturn(73000L);

        // Act
        BudgetProgressResponse result = budgetService.getProgress(userId, 1L);

        // Assert
        assertThat(result.getSpentPercentage()).isEqualTo(73);
    }

    // --- DELETE ---

    @Test
    void deleteBudget_whenNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        when(budgetRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> budgetService.deleteBudget(userId, 1L));
    }

    @Test
    void deleteBudget_whenFound_shouldDelete() {
        // Arrange
        when(budgetRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(budget));

        // Act
        budgetService.deleteBudget(userId, 1L);

        // Assert
        verify(budgetRepository).delete(budget);
    }

    // --- EDGE CASES ---

    @Test
    void getProgress_whenLimitAmountIsZero_shouldNotDivideByZero() {
        // Arrange
        budget.setLimitAmount(0L);
        when(budgetRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(budget));
        when(transactionRepository.calculateSpentAmountForCategoryAndMonth(anyLong(), anyLong(), any(), any(), any()))
                .thenReturn(50000L);

        // Act
        BudgetProgressResponse result = budgetService.getProgress(userId, 1L);

        // Assert
        assertThat(result.getSpentPercentage()).isEqualTo(0);
    }

    @Test
    void createOrUpdate_withSystemCategory_shouldAllowBudgetCreation() {
        // Arrange
        Category systemCat = Category.builder()
                .id(5L)
                .isSystem(true)
                .userId(0L)
                .name("System")
                .type(CategoryType.DEPENSE)
                .build();
        validRequest.setCategoryId(5L);

        when(categoryRepository.findByIdAndUserIdOrSystem(5L, userId)).thenReturn(Optional.of(systemCat));
        when(budgetRepository.findByUserIdAndCategoryIdAndBudgetYearAndBudgetMonth(anyLong(), anyLong(), anyInt(), anyInt()))
                .thenReturn(Optional.empty());
        when(budgetRepository.save(any(Budget.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        BudgetResponse response = budgetService.createOrUpdate(userId, validRequest);

        // Assert
        assertThat(response).isNotNull();
        verify(budgetRepository).save(any(Budget.class));
    }

    // --- IDOR SECURITY ---

    @Test
    void getProgress_userId2CannotReadUserId1BudgetProgress() {
        // Arrange
        Long userId2 = 2L;
        when(budgetRepository.findByIdAndUserId(budget.getId(), userId2)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> budgetService.getProgress(userId2, budget.getId()));
    }

    @Test
    void deleteBudget_userId2CannotDeleteUserId1Budget() {
        // Arrange
        Long userId2 = 2L;
        when(budgetRepository.findByIdAndUserId(budget.getId(), userId2)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> budgetService.deleteBudget(userId2, budget.getId()));
        verify(budgetRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Explicit IDOR: User B cannot update User A's budget category")
    void explicitIdorTest_userBCannotUpdateUserABudget() {
        Long userB = 2L;
        // userB does not own the category
        when(categoryRepository.findByIdAndUserIdOrSystem(validRequest.getCategoryId(), userB)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> budgetService.createOrUpdate(userB, validRequest));
    }

    /**
     * C-3 regression: When the budget upsert race window survives the PESSIMISTIC_WRITE lock
     * (e.g., two threads started within the same gap before the lock was taken), the DB unique
     * constraint fires and Spring wraps it in DataIntegrityViolationException.
     *
     * The service must NOT catch this — it must propagate to GlobalExceptionHandler which maps it
     * to 409. This test verifies the service layer is not accidentally swallowing it.
     *
     * Old code: DataIntegrityViolationException fell through to the generic Exception handler
     * → 500.  After the Batch 1 handler is in place, the same propagation now yields 409.
     *
     * This test would have PASSED on old service code but the overall HTTP response was 500.
     * It now documents the expected propagation contract at the service layer.
     */
    @Test
    @DisplayName("C-3: DataIntegrityViolationException from save() propagates uncaught (GlobalExceptionHandler maps it to 409)")
    void createOrUpdate_whenRaceConditionCausesUniqueConstraintViolation_shouldPropagateDataIntegrityException() {
        // Arrange — both threads see Optional.empty() (race window)
        when(categoryRepository.findByIdAndUserIdOrSystem(validRequest.getCategoryId(), userId))
                .thenReturn(Optional.of(cat));
        when(budgetRepository.findByUserIdAndCategoryIdAndBudgetYearAndBudgetMonth(
                anyLong(), anyLong(), anyInt(), anyInt()))
                .thenReturn(Optional.empty()); // Both threads see empty
        when(budgetRepository.save(any(Budget.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("unique_budget constraint"));

        // Act & Assert — the exception must propagate to be caught by GlobalExceptionHandler
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> budgetService.createOrUpdate(userId, validRequest));
    }
}
