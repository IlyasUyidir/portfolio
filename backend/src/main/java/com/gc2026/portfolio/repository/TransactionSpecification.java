package com.gc2026.portfolio.repository;

import com.gc2026.portfolio.domain.entity.Transaction;
import com.gc2026.portfolio.domain.enums.TransactionType;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
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

        Specification<Transaction> spec = Specification.where(hasUserId(userId))
                .and(isNotDeleted());

        if (startDate != null) {
            spec = spec.and(hasStartDate(startDate));
        }
        if (endDate != null) {
            spec = spec.and(hasEndDate(endDate));
        }
        if (type != null) {
            spec = spec.and(hasType(type));
        }
        if (categoryId != null) {
            spec = spec.and(hasCategoryId(categoryId, userId));
        }
        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(titleContains(keyword));
        }

        return spec;
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

    /**
     * I-3: Category ownership check added.
     *
     * Old predicate: WHERE t.category_id = :categoryId
     * — allowed probing arbitrary category IDs to detect their existence system-wide.
     *
     * New predicate: WHERE t.category_id = :categoryId
     *               AND (t.category.userId = :userId OR t.category.isSystem = true)
     * — A user can only filter by categories they own or system categories.
     *   Filtering by another user's category ID now returns an empty result (not an error),
     *   which is the correct information-safe behavior per the synthesis report.
     *
     * Note: The Criteria API join is added only when categoryId is non-null, so there is
     * no performance impact on queries that don't filter by category.
     */
    private static Specification<Transaction> hasCategoryId(Long categoryId, Long userId) {
        if (categoryId == null) return null;
        return (root, query, cb) -> {
            // Use a LEFT JOIN to the category table so the ownership predicate can be applied.
            // This is safe because isNotDeleted() and hasUserId() already constrain the result set.
            Join<Object, Object> category = root.join("category", JoinType.LEFT);
            return cb.and(
                    cb.equal(category.get("id"), categoryId),
                    cb.or(
                            cb.equal(category.get("userId"), userId),
                            cb.equal(category.get("isSystem"), true)
                    )
            );
        };
    }

    private static Specification<Transaction> titleContains(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("title")), "%" + keyword.toLowerCase() + "%");
    }
}
