package com.gc2026.portfolio.service;

import com.gc2026.portfolio.domain.entity.Budget;
import com.gc2026.portfolio.domain.entity.Category;
import com.gc2026.portfolio.domain.enums.TransactionType;
import com.gc2026.portfolio.domain.exception.ResourceNotFoundException;
import com.gc2026.portfolio.domain.exception.ValidationException;
import com.gc2026.portfolio.dto.request.CreateBudgetRequest;
import com.gc2026.portfolio.dto.response.BudgetProgressResponse;
import com.gc2026.portfolio.dto.response.BudgetResponse;
import com.gc2026.portfolio.dto.response.CategoryResponse;
import com.gc2026.portfolio.repository.BudgetRepository;
import com.gc2026.portfolio.repository.CategoryRepository;
import com.gc2026.portfolio.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public BudgetResponse createOrUpdate(Long userId, CreateBudgetRequest request) {
        if (request.getLimitAmount() <= 0) {
            throw new ValidationException("Budget limit must be strictly positive");
        }
        if (request.getBudgetMonth() < 1 || request.getBudgetMonth() > 12) {
            throw new ValidationException("Invalid month");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (category.getUserId() != null && !category.getUserId().equals(userId)) {
            throw new ValidationException("You cannot create a budget for this category");
        }

        Optional<Budget> existingBudget = budgetRepository.findByUserIdAndCategoryIdAndBudgetYearAndBudgetMonth(
                userId, category.getId(), request.getBudgetYear(), request.getBudgetMonth());

        Budget budget;
        if (existingBudget.isPresent()) {
            budget = existingBudget.get();
            budget.setLimitAmount(request.getLimitAmount());
        } else {
            budget = Budget.builder()
                    .userId(userId)
                    .category(category)
                    .budgetYear(request.getBudgetYear())
                    .budgetMonth(request.getBudgetMonth())
                    .limitAmount(request.getLimitAmount())
                    .alertThreshold(80)
                    .build();
        }

        budget = budgetRepository.save(budget);
        return mapToResponse(budget);
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgetsByMonth(Long userId, YearMonth month) {
        return budgetRepository.findByUserIdAndBudgetYearAndBudgetMonth(userId, month.getYear(), month.getMonthValue())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BudgetProgressResponse getProgress(Long userId, Long budgetId) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        LocalDate startDate = LocalDate.of(budget.getBudgetYear(), budget.getBudgetMonth(), 1);
        LocalDate endDate = YearMonth.of(budget.getBudgetYear(), budget.getBudgetMonth()).atEndOfMonth();

        Long spentAmount = transactionRepository.calculateSpentAmountForCategoryAndMonth(
                userId, budget.getCategory().getId(), startDate, endDate, TransactionType.DEPENSE);

        Long remainingAmount = Math.max(0L, budget.getLimitAmount() - spentAmount);

        int spentPercentage = 0;
        if (budget.getLimitAmount() > 0) {
            spentPercentage = (int) Math.round((double) spentAmount / budget.getLimitAmount() * 100);
        }

        String alertStatus = "NORMAL";
        if (spentPercentage >= 100) {
            alertStatus = "CRITICAL";
        } else if (spentPercentage >= budget.getAlertThreshold()) {
            alertStatus = "WARNING";
        }

        return BudgetProgressResponse.builder()
                .budget(mapToResponse(budget))
                .spentAmount(spentAmount)
                .remainingAmount(remainingAmount)
                .spentPercentage(spentPercentage)
                .alertStatus(alertStatus)
                .build();
    }

    private BudgetResponse mapToResponse(Budget budget) {
        return BudgetResponse.builder()
                .id(budget.getId())
                .userId(budget.getUserId())
                .category(CategoryResponse.builder()
                        .id(budget.getCategory().getId())
                        .name(budget.getCategory().getName())
                        .type(budget.getCategory().getType().name())
                        .color(budget.getCategory().getColor())
                        .isSystem(budget.getCategory().getUserId() == null)
                        .build())
                .budgetYear(budget.getBudgetYear())
                .budgetMonth(budget.getBudgetMonth())
                .limitAmount(budget.getLimitAmount())
                .alertThreshold(budget.getAlertThreshold())
                .createdAt(budget.getCreatedAt())
                .build();
    }
}
