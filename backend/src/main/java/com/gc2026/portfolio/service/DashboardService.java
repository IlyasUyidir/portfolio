package com.gc2026.portfolio.service;

import com.gc2026.portfolio.domain.enums.TransactionType;
import com.gc2026.portfolio.dto.response.CategorySpendingResponse;
import com.gc2026.portfolio.dto.response.DashboardKpiResponse;
import com.gc2026.portfolio.repository.CategorySpendingProjection;
import com.gc2026.portfolio.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public DashboardKpiResponse getKpis(Long userId, YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        Long totalIncome = transactionRepository.sumAmountByTypeAndDateRange(userId, TransactionType.REVENU, startDate, endDate);
        Long totalExpenses = transactionRepository.sumAmountByTypeAndDateRange(userId, TransactionType.DEPENSE, startDate, endDate);

        Long monthlyBalance = totalIncome - totalExpenses;
        
        Double savingsRate = 0.0;
        if (totalIncome > 0) {
            savingsRate = ((double) monthlyBalance / totalIncome) * 100.0;
            if (savingsRate < 0) {
                savingsRate = 0.0;
            }
        }

        return DashboardKpiResponse.builder()
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .monthlyBalance(monthlyBalance)
                .savingsRate(savingsRate)
                .build();
    }

    @Transactional(readOnly = true)
    public List<CategorySpendingResponse> getSpending(Long userId, YearMonth month) {
        LocalDate startDate;
        LocalDate endDate;

        if (month != null) {
            startDate = month.atDay(1);
            endDate = month.atEndOfMonth();
        } else {
            startDate = YearMonth.now().atDay(1);
            endDate = YearMonth.now().atEndOfMonth();
        }

        List<CategorySpendingProjection> projections = transactionRepository.getTopSpendingCategories(userId, startDate, endDate);

        List<CategorySpendingResponse> responses = new ArrayList<>();
        Long otherAmount = 0L;

        for (int i = 0; i < projections.size(); i++) {
            CategorySpendingProjection proj = projections.get(i);
            if (i < 8) {
                responses.add(CategorySpendingResponse.builder()
                        .categoryName(proj.getCategoryName())
                        .color(proj.getColor())
                        .amount(proj.getTotalAmount())
                        .build());
            } else {
                otherAmount += proj.getTotalAmount();
            }
        }

        if (otherAmount > 0) {
            responses.add(CategorySpendingResponse.builder()
                    .categoryName("Autre")
                    .color("#9CA3AF")
                    .amount(otherAmount)
                    .build());
        }

        return responses;
    }
}
