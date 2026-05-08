package com.gc2026.portfolio.repository;

import com.gc2026.portfolio.domain.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>,
        JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUserIdAndIsDeletedFalse(Long id, Long userId);

    boolean existsByCategoryId(Long categoryId);
}
