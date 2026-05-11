package com.gc2026.portfolio.service;

import com.gc2026.portfolio.domain.enums.TransactionType;
import com.gc2026.portfolio.dto.response.CategorySpendingResponse;
import com.gc2026.portfolio.dto.response.DashboardKpiResponse;
import com.gc2026.portfolio.repository.CategorySpendingProjection;
import com.gc2026.portfolio.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private Long userId;
    private YearMonth month;
    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {
        userId = 1L;
        month = YearMonth.of(2026, 5);
        startDate = month.atDay(1);
        endDate = month.atEndOfMonth();
    }

    // --- GET KPIs ---

    @Test
    void getKpis_whenNullIncome_shouldDefaultToZero() {
        // Arrange
        when(transactionRepository.sumAmountByTypeAndDateRange(eq(userId), eq(TransactionType.REVENU), any(), any()))
                .thenReturn(null);
        when(transactionRepository.sumAmountByTypeAndDateRange(eq(userId), eq(TransactionType.DEPENSE), any(), any()))
                .thenReturn(50000L);

        // Act
        DashboardKpiResponse result = dashboardService.getKpis(userId, month);

        // Assert
        assertThat(result.getTotalIncome()).isEqualTo(0L);
        assertThat(result.getTotalExpenses()).isEqualTo(50000L);
        assertThat(result.getMonthlyBalance()).isEqualTo(-50000L);
        assertThat(result.getSavingsRate()).isEqualTo(0.0);
    }

    @Test
    void getKpis_whenNullExpenses_shouldDefaultToZero() {
        // Arrange
        when(transactionRepository.sumAmountByTypeAndDateRange(eq(userId), eq(TransactionType.REVENU), any(), any()))
                .thenReturn(100000L);
        when(transactionRepository.sumAmountByTypeAndDateRange(eq(userId), eq(TransactionType.DEPENSE), any(), any()))
                .thenReturn(null);

        // Act
        DashboardKpiResponse result = dashboardService.getKpis(userId, month);

        // Assert
        assertThat(result.getTotalIncome()).isEqualTo(100000L);
        assertThat(result.getTotalExpenses()).isEqualTo(0L);
        assertThat(result.getMonthlyBalance()).isEqualTo(100000L);
        assertThat(result.getSavingsRate()).isEqualTo(100.0);
    }

    @Test
    void getKpis_shouldCalculateMonthlyBalanceCorrectly() {
        // Arrange
        when(transactionRepository.sumAmountByTypeAndDateRange(eq(userId), eq(TransactionType.REVENU), eq(startDate), eq(endDate)))
                .thenReturn(200000L);
        when(transactionRepository.sumAmountByTypeAndDateRange(eq(userId), eq(TransactionType.DEPENSE), eq(startDate), eq(endDate)))
                .thenReturn(150000L);

        // Act
        DashboardKpiResponse result = dashboardService.getKpis(userId, month);

        // Assert
        assertThat(result.getMonthlyBalance()).isEqualTo(50000L);
    }

    @Test
    void getKpis_whenExpensesExceedIncome_monthlyBalanceShouldBeNegative() {
        // Arrange
        when(transactionRepository.sumAmountByTypeAndDateRange(eq(userId), eq(TransactionType.REVENU), any(), any()))
                .thenReturn(100000L);
        when(transactionRepository.sumAmountByTypeAndDateRange(eq(userId), eq(TransactionType.DEPENSE), any(), any()))
                .thenReturn(150000L);

        // Act
        DashboardKpiResponse result = dashboardService.getKpis(userId, month);

        // Assert
        assertThat(result.getMonthlyBalance()).isEqualTo(-50000L);
        assertThat(result.getSavingsRate()).isEqualTo(0.0);
    }

    @Test
    void getKpis_shouldCalculateSavingsRateCorrectly() {
        // Arrange
        when(transactionRepository.sumAmountByTypeAndDateRange(eq(userId), eq(TransactionType.REVENU), any(), any()))
                .thenReturn(200000L);
        when(transactionRepository.sumAmountByTypeAndDateRange(eq(userId), eq(TransactionType.DEPENSE), any(), any()))
                .thenReturn(100000L);

        // Act
        DashboardKpiResponse result = dashboardService.getKpis(userId, month);

        // Assert
        assertThat(result.getSavingsRate()).isCloseTo(50.0, offset(0.01));
    }

    @Test
    void getKpis_whenIncomeIsZero_savingsRateShouldBeZero() {
        // Arrange
        when(transactionRepository.sumAmountByTypeAndDateRange(eq(userId), eq(TransactionType.REVENU), any(), any()))
                .thenReturn(0L);
        when(transactionRepository.sumAmountByTypeAndDateRange(eq(userId), eq(TransactionType.DEPENSE), any(), any()))
                .thenReturn(50000L);

        // Act
        DashboardKpiResponse result = dashboardService.getKpis(userId, month);

        // Assert
        assertThat(result.getSavingsRate()).isEqualTo(0.0);
    }

    @Test
    void getKpis_whenSavingsRateIsNegative_shouldClampToZero() {
        // Arrange
        when(transactionRepository.sumAmountByTypeAndDateRange(eq(userId), eq(TransactionType.REVENU), any(), any()))
                .thenReturn(100000L);
        when(transactionRepository.sumAmountByTypeAndDateRange(eq(userId), eq(TransactionType.DEPENSE), any(), any()))
                .thenReturn(200000L);

        // Act
        DashboardKpiResponse result = dashboardService.getKpis(userId, month);

        // Assert
        assertThat(result.getSavingsRate()).isEqualTo(0.0);
    }

    @Test
    void getKpis_shouldPassCorrectDateRangeToRepository() {
        // Arrange
        YearMonth specificMonth = YearMonth.of(2026, 5);
        LocalDate expectedStart = LocalDate.of(2026, 5, 1);
        LocalDate expectedEnd = LocalDate.of(2026, 5, 31);

        when(transactionRepository.sumAmountByTypeAndDateRange(any(), any(), any(), any())).thenReturn(0L);

        // Act
        dashboardService.getKpis(userId, specificMonth);

        // Assert
        verify(transactionRepository, times(1)).sumAmountByTypeAndDateRange(eq(userId), eq(TransactionType.REVENU), eq(expectedStart), eq(expectedEnd));
        verify(transactionRepository, times(1)).sumAmountByTypeAndDateRange(eq(userId), eq(TransactionType.DEPENSE), eq(expectedStart), eq(expectedEnd));
    }

    // --- GET SPENDING ---

    @Test
    void getSpending_whenLessThan8Categories_shouldReturnAll() {
        // Arrange
        List<CategorySpendingProjection> projections = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            projections.add(mockProjection("Cat " + i, "#00000" + i, 1000L * (i + 1)));
        }
        when(transactionRepository.getTopSpendingCategories(eq(userId), eq(startDate), eq(endDate)))
                .thenReturn(projections);

        // Act
        List<CategorySpendingResponse> result = dashboardService.getSpending(userId, month);

        // Assert
        assertThat(result).hasSize(5);
        assertThat(result.get(0).getCategory().getName()).isEqualTo("Cat 0");
        assertThat(result.get(4).getCategory().getName()).isEqualTo("Cat 4");
    }

    @Test
    void getSpending_whenMoreThan8Categories_shouldGroupRestIntoAutre() {
        // Arrange
        List<CategorySpendingProjection> projections = new ArrayList<>();
        // 10 projections with amounts 100, 90, ..., 10
        for (int i = 0; i < 10; i++) {
            projections.add(mockProjection("Cat " + i, "#Color", (long) (100 - (i * 10))));
        }
        when(transactionRepository.getTopSpendingCategories(eq(userId), eq(startDate), eq(endDate)))
                .thenReturn(projections);

        // Act
        List<CategorySpendingResponse> result = dashboardService.getSpending(userId, month);

        // Assert
        assertThat(result).hasSize(9); // 8 top + 1 "Autre"
        assertThat(result.get(8).getCategory().getName()).isEqualTo("Autre");
        assertThat(result.get(8).getTotalAmount()).isEqualTo(20L + 10L); // Cat 8 (20) + Cat 9 (10)
    }

    @Test
    void getSpending_whenMonthIsNull_shouldDefaultToCurrentMonth() {
        // Arrange
        YearMonth currentMonth = YearMonth.now();
        LocalDate expectedStart = currentMonth.atDay(1);
        LocalDate expectedEnd = currentMonth.atEndOfMonth();

        when(transactionRepository.getTopSpendingCategories(eq(userId), eq(expectedStart), eq(expectedEnd)))
                .thenReturn(new ArrayList<>());

        // Act
        dashboardService.getSpending(userId, null);

        // Assert
        verify(transactionRepository).getTopSpendingCategories(eq(userId), eq(expectedStart), eq(expectedEnd));
    }

    @Test
    void getSpending_autreCategoryShouldHaveCorrectColor() {
        // Arrange
        List<CategorySpendingProjection> projections = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            projections.add(mockProjection("Cat " + i, "#Color", 100L));
        }
        when(transactionRepository.getTopSpendingCategories(eq(userId), eq(startDate), eq(endDate)))
                .thenReturn(projections);

        // Act
        List<CategorySpendingResponse> result = dashboardService.getSpending(userId, month);

        // Assert
        CategorySpendingResponse autre = result.stream()
                .filter(r -> r.getCategory().getName().equals("Autre"))
                .findFirst()
                .orElseThrow();
        assertThat(autre.getCategory().getColor()).isEqualTo("#9CA3AF");
    }

    private CategorySpendingProjection mockProjection(String name, String color, Long amount) {
        CategorySpendingProjection proj = mock(CategorySpendingProjection.class);
        lenient().when(proj.getCategoryName()).thenReturn(name);
        lenient().when(proj.getColor()).thenReturn(color);
        when(proj.getTotalAmount()).thenReturn(amount);
        return proj;
    }
}
