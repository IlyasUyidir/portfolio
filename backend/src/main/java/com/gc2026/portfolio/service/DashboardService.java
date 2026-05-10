package com.gc2026.portfolio.service;

import com.gc2026.portfolio.domain.enums.TransactionType;
import com.gc2026.portfolio.dto.response.CategoryResponse;
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

        // 1. Fetch raw Object Longs (which might be null)
        Long rawIncome = transactionRepository.sumAmountByTypeAndDateRange(userId, TransactionType.REVENU, startDate,
                endDate);
        Long rawExpenses = transactionRepository.sumAmountByTypeAndDateRange(userId, TransactionType.DEPENSE, startDate,
                endDate);

        // 2. Safely unbox into primitive longs (guaranteed never to be null)
        long totalIncome = (rawIncome != null) ? rawIncome : 0L;
        long totalExpenses = (rawExpenses != null) ? rawExpenses : 0L;

        // 3. Math is now 100% safe
        long monthlyBalance = totalIncome - totalExpenses;

        // 4. Safe division
        double savingsRate = 0.0;
        if (totalIncome > 0) {
            savingsRate = ((double) monthlyBalance / (double) totalIncome) * 100.0;
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

        List<CategorySpendingProjection> projections = transactionRepository.getTopSpendingCategories(userId, startDate,
                endDate);

        List<CategorySpendingResponse> responses = new ArrayList<>();
        long otherAmount = 0L; // Use primitive long for safety

        for (int i = 0; i < projections.size(); i++) {
            CategorySpendingProjection proj = projections.get(i);
            if (i < 8) {
                CategoryResponse catResp = CategoryResponse.builder()
                        .name(proj.getCategoryName())
                        .color(proj.getColor())
                        .build();

                responses.add(CategorySpendingResponse.builder()
                        .category(catResp)
                        .totalAmount(proj.getTotalAmount())
                        .build());
            } else {
                otherAmount += proj.getTotalAmount();
            }
        }

        if (otherAmount > 0) {
            CategoryResponse autreCat = CategoryResponse.builder()
                    .name("Autre")
                    .color("#9CA3AF")
                    .build();

            responses.add(CategorySpendingResponse.builder()
                    .category(autreCat)
                    .totalAmount(otherAmount)
                    .build());
        }

        return responses;
    }
}