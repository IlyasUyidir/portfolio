package com.gc2026.portfolio.repository;

import com.gc2026.portfolio.domain.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByIdAndUserId(Long id, Long userId);

    List<Budget> findByUserIdAndBudgetYearAndBudgetMonth(Long userId, Integer budgetYear, Integer budgetMonth);

    Optional<Budget> findByUserIdAndCategoryIdAndBudgetYearAndBudgetMonth(
            Long userId, Long categoryId, Integer budgetYear, Integer budgetMonth);
}
