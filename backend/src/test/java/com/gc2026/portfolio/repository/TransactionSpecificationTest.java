package com.gc2026.portfolio.repository;

import com.gc2026.portfolio.domain.entity.Transaction;
import com.gc2026.portfolio.domain.enums.TransactionType;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionSpecificationTest {

    @Mock
    private Root<Transaction> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Path path;

    @Mock
    private Path titlePath;

    @Mock
    private Path categoryPath;

    @Mock
    private Path categoryIdPath;

    @Mock
    private Path categoryUserIdPath;

    @Mock
    private Path categoryIsSystemPath;

    @Mock
    private Join<Object, Object> categoryJoin;

    @Mock
    private Expression lowerTitle;

    @Mock
    private Predicate predicate;

    @BeforeEach
    void setUp() {
        // Default mocking for root.get()
        lenient().when(root.get(anyString())).thenReturn(path);
        lenient().when(root.get("title")).thenReturn(titlePath);

        // I-3: category ownership join mocking
        lenient().when(root.join("category", JoinType.LEFT)).thenReturn(categoryJoin);
        lenient().when(categoryJoin.get("id")).thenReturn(categoryIdPath);
        lenient().when(categoryJoin.get("userId")).thenReturn(categoryUserIdPath);
        lenient().when(categoryJoin.get("isSystem")).thenReturn(categoryIsSystemPath);

        // Specific mocking for category.id path (for old-style get)
        lenient().when(root.get("category")).thenReturn(categoryPath);
        lenient().when(categoryPath.get("id")).thenReturn(categoryIdPath);

        // Default mocking for cb methods to return a predicate
        lenient().when(cb.equal(any(), any())).thenReturn(predicate);
        lenient().doReturn(predicate).when(cb).greaterThanOrEqualTo(any(), any(LocalDate.class));
        lenient().doReturn(predicate).when(cb).lessThanOrEqualTo(any(), any(LocalDate.class));
        lenient().when(cb.like(any(), anyString())).thenReturn(predicate);
        lenient().when(cb.lower(any())).thenReturn(lowerTitle);
        lenient().when(cb.or(any(), any())).thenReturn(predicate);
        lenient().when(cb.and(any(), any())).thenReturn(predicate);
    }

    @Test
    void buildFilter_withAllNullParams_shouldReturnValidSpecification() {
        // Arrange
        Long userId = 1L;

        // Act
        Specification<Transaction> spec = TransactionSpecification.buildFilter(userId, null, null, null, null, null);
        spec.toPredicate(root, query, cb);

        // Assert
        assertThat(spec).isNotNull();
        verify(cb).equal(any(), eq(userId));
        verify(cb).equal(any(), eq(false));
    }

    @Test
    void buildFilter_withKeyword_shouldCreateLikePredicate() {
        // Arrange
        Long userId = 1L;
        String keyword = "salaire";

        // Act
        Specification<Transaction> spec = TransactionSpecification.buildFilter(userId, null, null, null, null, keyword);
        spec.toPredicate(root, query, cb);

        // Assert
        assertThat(spec).isNotNull();
        verify(cb).like(any(), eq("%salaire%"));
        verify(cb).lower(any());
    }

    @Test
    void buildFilter_withBlankKeyword_shouldIgnoreKeyword() {
        // Arrange
        Long userId = 1L;
        String keyword = "  ";

        // Act
        Specification<Transaction> spec = TransactionSpecification.buildFilter(userId, null, null, null, null, keyword);
        spec.toPredicate(root, query, cb);

        // Assert
        verify(cb, never()).like(any(), anyString());
    }

    @Test
    void buildFilter_withAllParams_shouldNotThrow() {
        // Arrange
        Long userId = 1L;
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();
        TransactionType type = TransactionType.DEPENSE;
        Long categoryId = 10L;
        String keyword = "courses";

        // Act
        Specification<Transaction> spec = TransactionSpecification.buildFilter(userId, start, end, type, categoryId, keyword);
        spec.toPredicate(root, query, cb);

        // Assert
        assertThat(spec).isNotNull();
        verify(cb, org.mockito.Mockito.times(2)).equal(any(), eq(userId)); // I-3: hasUserId + the ownership check inside hasCategoryId both compare userId
        verify(cb).equal(any(), eq(false));
        verify(cb).greaterThanOrEqualTo(any(), eq(start));
        verify(cb).lessThanOrEqualTo(any(), eq(end));
        verify(cb).equal(any(), eq(type));
        verify(cb).like(any(), eq("%courses%"));
    }

    @Test
    void buildFilter_userIdIsAlwaysRequired() {
        // Arrange
        Long userId = 999L;

        // Act
        Specification<Transaction> spec = TransactionSpecification.buildFilter(userId, null, null, null, null, null);
        spec.toPredicate(root, query, cb);

        // Assert
        verify(cb).equal(any(), eq(userId));
    }

    // ─── I-3 regression tests ─────────────────────────────────────────────────────

    /**
     * I-3 regression: When hasCategoryId is called, the predicate must include an
     * ownership check (userId or isSystem=true), NOT just category.id = :categoryId.
     *
     * Old code: only checked category.id = :categoryId — allowed probing any category ID.
     * New code: adds AND (category.userId = :userId OR category.isSystem = true).
     *
     * This test verifies the ownership join is executed when a categoryId filter is provided.
     * It would FAIL on old code (no join was performed).
     */
    @Test
    @DisplayName("I-3: hasCategoryId includes ownership check — join to category with userId or isSystem predicate")
    void buildFilter_withCategoryId_shouldJoinCategoryAndCheckOwnership() {
        // Arrange
        Long userId = 1L;
        Long categoryId = 42L;

        // Act
        Specification<Transaction> spec = TransactionSpecification.buildFilter(
                userId, null, null, null, categoryId, null);
        spec.toPredicate(root, query, cb);

        // Assert: the ownership join was performed
        verify(root).join("category", JoinType.LEFT);

        // Assert: cb.or() was called to build the (userId = ? OR isSystem = true) predicate
        verify(cb).or(any(), any());

        // Assert: cb.and() was called to combine category.id = ? AND (ownership predicate)
        verify(cb).and(any(), any());
    }

    /**
     * I-3 regression: When categoryId is null, NO ownership join should be performed —
     * the query should not touch the category table at all for the filter.
     */
    @Test
    @DisplayName("I-3: hasCategoryId with null categoryId skips the ownership join entirely")
    void buildFilter_withNullCategoryId_shouldNotJoinCategory() {
        // Arrange
        Long userId = 1L;

        // Act
        Specification<Transaction> spec = TransactionSpecification.buildFilter(
                userId, null, null, null, null, null);
        spec.toPredicate(root, query, cb);

        // Assert: no ownership join was performed
        verify(root, never()).join(eq("category"), any(JoinType.class));
        // Assert: no OR predicate was built (no isSystem check)
        verify(cb, never()).or(any(), any());
    }
}
