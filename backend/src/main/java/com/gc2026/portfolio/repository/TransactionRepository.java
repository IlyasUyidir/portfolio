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

    boolean existsByCategoryId(Long categoryId);

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

    List<Transaction> findAllByUserIdAndIsDeletedFalseOrderByTxDateDesc(Long userId);
}
