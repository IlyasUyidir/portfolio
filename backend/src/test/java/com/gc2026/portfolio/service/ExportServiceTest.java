package com.gc2026.portfolio.service;

import com.gc2026.portfolio.domain.entity.Category;
import com.gc2026.portfolio.domain.entity.Transaction;
import com.gc2026.portfolio.domain.enums.TransactionType;
import com.gc2026.portfolio.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private ExportService exportService;

    private Long userId;
    private Category cat;
    private Transaction tx1;
    private Transaction tx2;

    @BeforeEach
    void setUp() {
        userId = 1L;
        cat = Category.builder().id(1L).name("Alimentation").build();
        tx1 = Transaction.builder()
                .id(1L)
                .userId(userId)
                .title("Courses")
                .amount(85000L)
                .type(TransactionType.DEPENSE)
                .category(cat)
                .txDate(LocalDate.of(2026, 5, 10))
                .description(null)
                .isDeleted(false)
                .build();
        tx2 = Transaction.builder()
                .id(2L)
                .userId(userId)
                .title("Salaire, mensuel")
                .amount(1500000L)
                .type(TransactionType.REVENU)
                .category(cat)
                .txDate(LocalDate.of(2026, 5, 1))
                .description("Virement DRH")
                .isDeleted(false)
                .build();
    }

    @Test
    void exportToCsv_shouldIncludeHeaderRow() {
        // Arrange
        when(transactionRepository.findAllByUserIdAndIsDeletedFalseOrderByTxDateDesc(userId))
                .thenReturn(List.of(tx1));

        // Act
        String result = exportService.exportToCsv(userId);

        // Assert
        assertThat(result).startsWith("Date,Title,Type,Category,Amount(centimes),Description");
    }

    @Test
    void exportToCsv_shouldIncludeOneRowPerTransaction() {
        // Arrange
        when(transactionRepository.findAllByUserIdAndIsDeletedFalseOrderByTxDateDesc(userId))
                .thenReturn(List.of(tx1, tx2));

        // Act
        String result = exportService.exportToCsv(userId);

        // Assert
        String[] lines = result.split("\n");
        assertThat(lines).hasSize(3); // Header + 2 rows
    }

    @Test
    void exportToCsv_shouldFormatAmountInCentimes() {
        // Arrange
        when(transactionRepository.findAllByUserIdAndIsDeletedFalseOrderByTxDateDesc(userId))
                .thenReturn(List.of(tx1));

        // Act
        String result = exportService.exportToCsv(userId);

        // Assert
        assertThat(result).contains("85000");
    }

    @Test
    void exportToCsv_whenTitleContainsComma_shouldWrapInQuotes() {
        // Arrange
        when(transactionRepository.findAllByUserIdAndIsDeletedFalseOrderByTxDateDesc(userId))
                .thenReturn(List.of(tx2));

        // Act
        String result = exportService.exportToCsv(userId);

        // Assert
        assertThat(result).contains("\"Salaire, mensuel\"");
    }

    @Test
    void exportToCsv_whenDescriptionIsNull_shouldOutputEmptyString() {
        // Arrange
        when(transactionRepository.findAllByUserIdAndIsDeletedFalseOrderByTxDateDesc(userId))
                .thenReturn(List.of(tx1));

        // Act
        String result = exportService.exportToCsv(userId);

        // Assert
        // Row should end with a comma (for the null description) followed by a newline
        assertThat(result.trim()).endsWith(",");
    }

    @Test
    void exportToCsv_whenTitleContainsDoubleQuotes_shouldEscapeQuotes() {
        // Arrange
        Transaction txWithQuotes = Transaction.builder()
                .title("Transfer \"special\"")
                .amount(1000L)
                .build();
        when(transactionRepository.findAllByUserIdAndIsDeletedFalseOrderByTxDateDesc(userId))
                .thenReturn(List.of(txWithQuotes));

        // Act
        String result = exportService.exportToCsv(userId);

        // Assert
        assertThat(result).contains("\"Transfer \"\"special\"\"\"");
    }

    @Test
    void exportToCsv_whenNoTransactions_shouldOnlyReturnHeader() {
        // Arrange
        when(transactionRepository.findAllByUserIdAndIsDeletedFalseOrderByTxDateDesc(userId))
                .thenReturn(Collections.emptyList());

        // Act
        String result = exportService.exportToCsv(userId);

        // Assert
        String[] lines = result.split("\n");
        assertThat(lines).hasSize(1);
        assertThat(lines[0]).isEqualTo("Date,Title,Type,Category,Amount(centimes),Description");
    }

    @Test
    void exportToCsv_onlyExportsNonDeletedTransactions() {
        // Arrange
        when(transactionRepository.findAllByUserIdAndIsDeletedFalseOrderByTxDateDesc(userId))
                .thenReturn(List.of(tx1));

        // Act
        exportService.exportToCsv(userId);

        // Assert
        verify(transactionRepository).findAllByUserIdAndIsDeletedFalseOrderByTxDateDesc(userId);
    }
}
