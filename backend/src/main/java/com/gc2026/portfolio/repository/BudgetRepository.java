package com.gc2026.portfolio.repository;

import com.gc2026.portfolio.domain.entity.Budget;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = {"category"})
    List<Budget> findByUserIdAndBudgetYearAndBudgetMonth(Long userId, Integer budgetYear, Integer budgetMonth);

    /**
     * C-3: PESSIMISTIC_WRITE lock on the budget lookup used in createOrUpdate().
     *
     * When the Optional is empty (new budget), this query takes a gap-lock (PostgreSQL)
     * on the index range covering (userId, categoryId, year, month), which serialises
     * concurrent inserts for the same key tuple.  The second thread blocks until the
     * first thread's transaction commits.  If the first thread inserts, the second
     * thread's find will now return non-empty and it will UPDATE instead of INSERT —
     * eliminating the race.  The remaining window (two threads simultaneously see empty
     * and both commit) is covered by the DB unique constraint + the
     * DataIntegrityViolationException → 409 handler added in Batch 1.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Budget b WHERE b.userId = :userId AND b.category.id = :categoryId " +
           "AND b.budgetYear = :budgetYear AND b.budgetMonth = :budgetMonth")
    Optional<Budget> findByUserIdAndCategoryIdAndBudgetYearAndBudgetMonth(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("budgetYear") Integer budgetYear,
            @Param("budgetMonth") Integer budgetMonth);
}
