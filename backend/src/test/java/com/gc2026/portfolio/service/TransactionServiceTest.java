package com.gc2026.portfolio.service;

import com.gc2026.portfolio.domain.entity.Category;
import com.gc2026.portfolio.domain.entity.Transaction;
import com.gc2026.portfolio.domain.enums.CategoryType;
import com.gc2026.portfolio.domain.enums.TransactionType;
import com.gc2026.portfolio.domain.exception.ResourceNotFoundException;
import com.gc2026.portfolio.dto.request.CreateTransactionRequest;
import com.gc2026.portfolio.dto.request.UpdateTransactionRequest;
import com.gc2026.portfolio.dto.response.PaginatedResponse;
import com.gc2026.portfolio.dto.response.TransactionResponse;
import com.gc2026.portfolio.repository.CategoryRepository;
import com.gc2026.portfolio.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Long userId = 1L;
    private Category category;
    private Transaction tx;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .id(10L)
                .userId(1L)
                .name("Alimentation")
                .type(CategoryType.DEPENSE)
                .isSystem(true)
                .color("#EF4444")
                .build();

        tx = Transaction.builder()
                .id(100L)
                .userId(1L)
                .title("Courses")
                .amount(50000L)
                .type(TransactionType.DEPENSE)
                .category(category)
                .txDate(LocalDate.of(2026, 5, 10))
                .isDeleted(false)
                .build();
    }

    // --- CREATE ---

    @Test
    void create_whenCategoryNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .categoryId(99L)
                .build();
        when(categoryRepository.findByIdAndUserIdOrSystem(99L, userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> transactionService.create(userId, request));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void create_whenCategoryIsSystemCategory_shouldAllowCreation() {
        // Arrange
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .title("Test")
                .amount(1000L)
                .type(TransactionType.DEPENSE)
                .categoryId(10L)
                .txDate(LocalDate.now())
                .build();
        
        Category systemCategory = Category.builder()
                .id(10L)
                .userId(99L) // Different user
                .isSystem(true)
                .name("System Cat")
                .type(CategoryType.DEPENSE)
                .build();

        when(categoryRepository.findByIdAndUserIdOrSystem(10L, userId)).thenReturn(Optional.of(systemCategory));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        TransactionResponse response = transactionService.create(userId, request);

        // Assert
        assertThat(response).isNotNull();
        verify(transactionRepository).save(any());
    }

    @Test
    void create_shouldSetIsDeletedFalseByDefault() {
        // Arrange
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .title("Test")
                .amount(1000L)
                .type(TransactionType.DEPENSE)
                .categoryId(10L)
                .txDate(LocalDate.now())
                .build();

        when(categoryRepository.findByIdAndUserIdOrSystem(10L, userId)).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        transactionService.create(userId, request);

        // Assert
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getIsDeleted()).isFalse();
    }

    @Test
    void create_shouldPersistAllFieldsCorrectly() {
        // Arrange
        LocalDate now = LocalDate.now();
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .title("Salaire")
                .amount(500000L)
                .type(TransactionType.REVENU)
                .categoryId(10L)
                .txDate(now)
                .description("Paycheck")
                .build();

        when(categoryRepository.findByIdAndUserIdOrSystem(10L, userId)).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        transactionService.create(userId, request);

        // Assert
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        Transaction saved = captor.getValue();
        
        assertThat(saved.getTitle()).isEqualTo("Salaire");
        assertThat(saved.getAmount()).isEqualTo(500000L);
        assertThat(saved.getType()).isEqualTo(TransactionType.REVENU);
        assertThat(saved.getTxDate()).isEqualTo(now);
        assertThat(saved.getDescription()).isEqualTo("Paycheck");
        assertThat(saved.getUserId()).isEqualTo(userId);
    }

    @Test
    void create_withNullDescription_shouldNotThrow() {
        // Arrange
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .title("Test")
                .amount(1000L)
                .type(TransactionType.DEPENSE)
                .categoryId(10L)
                .txDate(LocalDate.now())
                .description(null)
                .build();

        when(categoryRepository.findByIdAndUserIdOrSystem(10L, userId)).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        transactionService.create(userId, request);

        // Assert
        verify(transactionRepository).save(any());
    }

    @Test
    void create_withMaxTitleLength_shouldSucceed() {
        // Arrange
        String longTitle = "A".repeat(255);
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .title(longTitle)
                .amount(1000L)
                .type(TransactionType.DEPENSE)
                .categoryId(10L)
                .txDate(LocalDate.now())
                .build();

        when(categoryRepository.findByIdAndUserIdOrSystem(10L, userId)).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        TransactionResponse response = transactionService.create(userId, request);

        // Assert
        assertThat(response.getTitle()).isEqualTo(longTitle);
    }

    @Test
    void create_withAmount1Centime_shouldSucceed() {
        // Arrange
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .title("Minor")
                .amount(1L)
                .type(TransactionType.DEPENSE)
                .categoryId(10L)
                .txDate(LocalDate.now())
                .build();

        when(categoryRepository.findByIdAndUserIdOrSystem(10L, userId)).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        TransactionResponse response = transactionService.create(userId, request);

        // Assert
        assertThat(response.getAmount()).isEqualTo(1L);
    }

    @Test
    void create_withAmountLongMaxValue_shouldNotOverflow() {
        // Arrange
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .title("Huge")
                .amount(Long.MAX_VALUE)
                .type(TransactionType.DEPENSE)
                .categoryId(10L)
                .txDate(LocalDate.now())
                .build();

        when(categoryRepository.findByIdAndUserIdOrSystem(10L, userId)).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        TransactionResponse response = transactionService.create(userId, request);

        // Assert
        assertThat(response.getAmount()).isEqualTo(Long.MAX_VALUE);
    }

    // --- UPDATE ---

    @Test
    void update_whenTransactionNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        UpdateTransactionRequest request = UpdateTransactionRequest.builder().build();
        when(transactionRepository.findByIdAndUserIdAndIsDeletedFalse(100L, userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> transactionService.update(userId, 100L, request));
    }

    @Test
    void update_whenCategoryNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        UpdateTransactionRequest request = UpdateTransactionRequest.builder()
                .categoryId(99L)
                .build();
        when(transactionRepository.findByIdAndUserIdAndIsDeletedFalse(100L, userId)).thenReturn(Optional.of(tx));
        when(categoryRepository.findByIdAndUserIdOrSystem(99L, userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> transactionService.update(userId, 100L, request));
    }

    @Test
    void update_shouldModifyAllFieldsAndSave() {
        // Arrange
        LocalDate newDate = LocalDate.now();
        UpdateTransactionRequest request = UpdateTransactionRequest.builder()
                .title("New Title")
                .amount(60000L)
                .type(TransactionType.REVENU)
                .categoryId(10L)
                .txDate(newDate)
                .description("New Desc")
                .build();

        when(transactionRepository.findByIdAndUserIdAndIsDeletedFalse(100L, userId)).thenReturn(Optional.of(tx));
        when(categoryRepository.findByIdAndUserIdOrSystem(10L, userId)).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        transactionService.update(userId, 100L, request);

        // Assert
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        Transaction saved = captor.getValue();

        assertThat(saved.getTitle()).isEqualTo("New Title");
        assertThat(saved.getAmount()).isEqualTo(60000L);
        assertThat(saved.getType()).isEqualTo(TransactionType.REVENU);
        assertThat(saved.getTxDate()).isEqualTo(newDate);
        assertThat(saved.getDescription()).isEqualTo("New Desc");
    }

    // --- DELETE ---

    @Test
    void delete_whenTransactionBelongsToUser_shouldSetIsDeletedTrue() {
        // Arrange
        when(transactionRepository.findByIdAndUserIdAndIsDeletedFalse(100L, userId)).thenReturn(Optional.of(tx));

        // Act
        transactionService.delete(userId, 100L);

        // Assert
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getIsDeleted()).isTrue();
        verify(transactionRepository, never()).delete(any(Transaction.class));
    }

    @Test
    void delete_whenTransactionNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        when(transactionRepository.findByIdAndUserIdAndIsDeletedFalse(100L, userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> transactionService.delete(userId, 100L));
        verify(transactionRepository, never()).delete(any(Transaction.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void delete_whenTransactionAlreadyDeleted_shouldThrowResourceNotFoundException() {
        // Arrange
        // Repo returns empty because it filters for isDeleted = false
        when(transactionRepository.findByIdAndUserIdAndIsDeletedFalse(100L, userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> transactionService.delete(userId, 100L));
    }

    @Test
    void delete_whenCalledTwice_secondCallShouldThrow() {
        // Arrange
        when(transactionRepository.findByIdAndUserIdAndIsDeletedFalse(100L, userId))
                .thenReturn(Optional.of(tx)) // First call
                .thenReturn(Optional.empty()); // Second call

        // Act
        transactionService.delete(userId, 100L); // First call succeeds

        // Assert second call throws
        assertThrows(ResourceNotFoundException.class, () -> transactionService.delete(userId, 100L));
    }

    // --- GET BY ID ---

    @Test
    void getById_whenTransactionExistsAndBelongsToUser_shouldReturnResponse() {
        // Arrange
        when(transactionRepository.findByIdAndUserIdAndIsDeletedFalse(100L, userId)).thenReturn(Optional.of(tx));

        // Act
        TransactionResponse response = transactionService.getById(userId, 100L);

        // Assert
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getTitle()).isEqualTo("Courses");
    }

    @Test
    void getById_whenTransactionBelongsToOtherUser_shouldThrowResourceNotFoundException() {
        // Arrange
        when(transactionRepository.findByIdAndUserIdAndIsDeletedFalse(100L, userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> transactionService.getById(userId, 100L));
    }

    // --- LIST ---

    @Test
    void list_shouldPassAllFiltersToSpecificationAndReturnPaginatedResponse() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transaction> page = new PageImpl<>(List.of(tx, tx));
        
        when(transactionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        // Act
        PaginatedResponse<TransactionResponse> response = transactionService.list(
                userId, null, null, null, null, null, pageable);

        // Assert
        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getContent()).hasSize(2);
        verify(transactionRepository).findAll(any(Specification.class), eq(pageable));
    }

    // --- IDOR SECURITY ---

    @Test
    void getById_userId2CannotReadUserId1Transaction() {
        // Arrange
        Long userId2 = 2L;
        when(transactionRepository.findByIdAndUserIdAndIsDeletedFalse(100L, userId2)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> transactionService.getById(userId2, 100L));
    }

    @Test
    void delete_userId2CannotDeleteUserId1Transaction() {
        // Arrange
        Long userId2 = 2L;
        when(transactionRepository.findByIdAndUserIdAndIsDeletedFalse(100L, userId2)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> transactionService.delete(userId2, 100L));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void update_userId2CannotUpdateUserId1Transaction() {
        // Arrange
        Long userId2 = 2L;
        UpdateTransactionRequest request = UpdateTransactionRequest.builder().build();
        when(transactionRepository.findByIdAndUserIdAndIsDeletedFalse(100L, userId2)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> transactionService.update(userId2, 100L, request));
    }

    @Test
    void createTransaction_userId1CannotUseUserId2PrivateCategory() {
        // Arrange
        Long userId1 = 1L;
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .categoryId(20L)
                .build();
        
        // Mock returning empty because Category 20 belongs to User 2 and is NOT system
        when(categoryRepository.findByIdAndUserIdOrSystem(20L, userId1)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> transactionService.create(userId1, request));
    }

    @Test
    void createTransaction_userId1CAN_useSystemCategory() {
        // Arrange
        Long userId1 = 1L;
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .title("Test")
                .amount(1000L)
                .type(TransactionType.DEPENSE)
                .categoryId(10L)
                .txDate(LocalDate.now())
                .build();
        
        Category systemCategory = Category.builder()
                .id(10L)
                .userId(99L) // Belongs to another user or 0
                .isSystem(true)
                .type(CategoryType.DEPENSE)
                .build();

        when(categoryRepository.findByIdAndUserIdOrSystem(10L, userId1)).thenReturn(Optional.of(systemCategory));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act & Assert
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> transactionService.create(userId1, request));
        verify(transactionRepository).save(any());
    }
}
