package com.gc2026.portfolio.repository;

import com.gc2026.portfolio.domain.entity.Transaction;
import com.gc2026.portfolio.domain.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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
}
