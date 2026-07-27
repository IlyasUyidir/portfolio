package com.gc2026.portfolio.repository;

import com.gc2026.portfolio.domain.entity.Transaction;
import com.gc2026.portfolio.domain.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>,
        JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUserIdAndIsDeletedFalse(Long id, Long userId);

    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"category"})
    org.springframework.data.domain.Page<Transaction> findAll(org.springframework.data.jpa.domain.Specification<Transaction> spec, org.springframework.data.domain.Pageable pageable);

    /**
     * C-2: Was existsByCategoryId — which included soft-deleted transactions,
     * permanently blocking users from deleting categories after soft-deleting all transactions.
     * Fixed to only count non-deleted transactions.
     */
    boolean existsByCategoryIdAndIsDeletedFalse(Long categoryId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.userId = :userId " +
           "AND t.category.id = :categoryId " +
           "AND t.txDate >= :startDate " +
           "AND t.txDate <= :endDate " +
           "AND t.type = :type " +
           "AND t.isDeleted = false")
    Long calculateSpentAmountForCategoryAndMonth(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("type") TransactionType type);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.userId = :userId " +
           "AND t.type = :type " +
           "AND t.txDate >= :startDate " +
           "AND t.txDate <= :endDate " +
           "AND t.isDeleted = false")
    Long sumAmountByTypeAndDateRange(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    interface IncomeExpenseProjection {
        Long getIncome();
        Long getExpenses();
    }

    @Query("SELECT " +
           "COALESCE(SUM(CASE WHEN t.type = 'REVENU' THEN t.amount ELSE 0 END), 0) as income, " +
           "COALESCE(SUM(CASE WHEN t.type = 'DEPENSE' THEN t.amount ELSE 0 END), 0) as expenses " +
           "FROM Transaction t " +
           "WHERE t.userId = :userId " +
           "AND t.txDate >= :startDate " +
           "AND t.txDate <= :endDate " +
           "AND t.isDeleted = false")
    IncomeExpenseProjection getIncomeAndExpenses(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT c.name as categoryName, c.color as color, SUM(t.amount) as totalAmount " +
           "FROM Transaction t JOIN t.category c " +
           "WHERE t.userId = :userId " +
           "AND t.type = 'DEPENSE' " +
           "AND t.txDate >= :startDate " +
           "AND t.txDate <= :endDate " +
           "AND t.isDeleted = false " +
           "GROUP BY c.id, c.name, c.color " +
           "ORDER BY totalAmount DESC")
    List<CategorySpendingProjection> getTopSpendingCategories(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * C-5 (scope-adjusted): JOIN FETCH category eliminates the N+1 avalanche that fired
     * 100k SELECT statements per export call. The full streaming rewrite (Pageable loop +
     * HttpServletResponse streaming) is deliberately deferred.
     *
     * TODO: full streaming rewrite if transaction volumes grow past ~10k per user.
     * Switch to a chunked Pageable loop writing directly to HttpServletResponse.getOutputStream()
     * rather than accumulating the full CSV in heap.
     */
    @Query("SELECT t FROM Transaction t JOIN FETCH t.category " +
           "WHERE t.userId = :userId AND t.isDeleted = false " +
           "ORDER BY t.txDate DESC")
    List<Transaction> findAllByUserIdAndIsDeletedFalseOrderByTxDateDesc(
            @Param("userId") Long userId);
}
