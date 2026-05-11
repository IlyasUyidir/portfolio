package com.gc2026.portfolio.repository;

import com.gc2026.portfolio.domain.entity.Transaction;
import com.gc2026.portfolio.domain.enums.TransactionType;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
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
    private Expression lowerTitle;

    @Mock
    private Predicate predicate;

    @BeforeEach
    void setUp() {
        // Default mocking for root.get()
        lenient().when(root.get(anyString())).thenReturn(path);
        lenient().when(root.get("title")).thenReturn(titlePath);
        
        // Specific mocking for category.id path
        lenient().when(root.get("category")).thenReturn(categoryPath);
        lenient().when(categoryPath.get("id")).thenReturn(categoryIdPath);

        // Default mocking for cb methods to return a predicate
        lenient().when(cb.equal(any(), any())).thenReturn(predicate);
        lenient().doReturn(predicate).when(cb).greaterThanOrEqualTo(any(), any(LocalDate.class));
        lenient().doReturn(predicate).when(cb).lessThanOrEqualTo(any(), any(LocalDate.class));
        lenient().when(cb.like(any(), anyString())).thenReturn(predicate);
        lenient().when(cb.lower(any())).thenReturn(lowerTitle);
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
        verify(cb).equal(any(), eq(userId));
        verify(cb).equal(any(), eq(false));
        verify(cb).greaterThanOrEqualTo(any(), eq(start));
        verify(cb).lessThanOrEqualTo(any(), eq(end));
        verify(cb).equal(any(), eq(type));
        verify(cb).equal(any(), eq(categoryId));
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

}
