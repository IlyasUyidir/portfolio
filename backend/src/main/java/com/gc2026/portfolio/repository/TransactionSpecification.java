package com.gc2026.portfolio.repository;

import com.gc2026.portfolio.domain.entity.Transaction;
import com.gc2026.portfolio.domain.enums.TransactionType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * Builds dynamic WHERE clauses for transaction queries.
 * userId and isDeleted=false are always applied.
 */
public final class TransactionSpecification {

    private TransactionSpecification() {
        // utility class
    }

    public static Specification<Transaction> buildFilter(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            TransactionType type,
            Long categoryId,
            String keyword) {

        return Specification
                .where(hasUserId(userId))
                .and(isNotDeleted())
                .and(hasStartDate(startDate))
                .and(hasEndDate(endDate))
                .and(hasType(type))
                .and(hasCategoryId(categoryId))
                .and(titleContains(keyword));
    }

    private static Specification<Transaction> hasUserId(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    private static Specification<Transaction> isNotDeleted() {
        return (root, query, cb) -> cb.equal(root.get("isDeleted"), false);
    }

    private static Specification<Transaction> hasStartDate(LocalDate startDate) {
        if (startDate == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("txDate"), startDate);
    }

    private static Specification<Transaction> hasEndDate(LocalDate endDate) {
        if (endDate == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("txDate"), endDate);
    }

    private static Specification<Transaction> hasType(TransactionType type) {
        if (type == null) return null;
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    private static Specification<Transaction> hasCategoryId(Long categoryId) {
        if (categoryId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    private static Specification<Transaction> titleContains(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("title")), "%" + keyword.toLowerCase() + "%");
    }
}
