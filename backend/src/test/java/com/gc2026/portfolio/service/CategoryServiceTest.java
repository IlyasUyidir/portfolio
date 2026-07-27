package com.gc2026.portfolio.service;

import com.gc2026.portfolio.domain.entity.Category;
import com.gc2026.portfolio.domain.enums.CategoryType;
import com.gc2026.portfolio.domain.exception.ResourceNotFoundException;
import com.gc2026.portfolio.domain.exception.ValidationException;
import com.gc2026.portfolio.dto.request.CreateCategoryRequest;
import com.gc2026.portfolio.dto.request.UpdateCategoryRequest;
import com.gc2026.portfolio.dto.response.CategoryResponse;
import com.gc2026.portfolio.repository.CategoryRepository;
import com.gc2026.portfolio.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Long userId = 1L;
    private Category customCat;
    private Category systemCat;

    @BeforeEach
    void setUp() {
        customCat = Category.builder()
                .id(5L)
                .userId(userId)
                .name("Voyage")
                .type(CategoryType.DEPENSE)
                .color("#8B5CF6")
                .isSystem(false)
                .build();

        systemCat = Category.builder()
                .id(1L)
                .userId(userId)
                .name("Alimentation")
                .type(CategoryType.DEPENSE)
                .color("#EF4444")
                .isSystem(true)
                .build();
    }

    // --- CREATE ---

    @Test
    @DisplayName("create - should throw ValidationException when STANDARD user reaches limit")
    void create_whenStandardUserReachesLimit_shouldThrowValidationException() {
        // Arrange
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("New Cat").type(CategoryType.DEPENSE).color("#000000").build();
        // I-2: stub the new locked count method
        when(categoryRepository.countByUserIdAndIsSystemFalseForUpdate(userId)).thenReturn(10L);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
                () -> categoryService.create(userId, "STANDARD", request));
        
        assertThat(exception.getMessage()).contains("10").contains("Standard");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("create - should succeed when PREMIUM user exceeds standard limit")
    void create_whenPremiumUserExceedsStandardLimit_shouldSucceed() {
        // Arrange
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("New Cat").type(CategoryType.DEPENSE).color("#000000").build();
        when(categoryRepository.existsByUserIdAndNameIgnoreCase(userId, "New Cat")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(customCat);

        // Act
        CategoryResponse result = categoryService.create(userId, "PREMIUM", request);

        // Assert
        assertThat(result).isNotNull();
        verify(categoryRepository, never()).countByUserIdAndIsSystemFalseForUpdate(any());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("create - should throw ValidationException when duplicate name exists")
    void create_whenDuplicateNameExists_shouldThrowValidationException() {
        // Arrange
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Voyage").type(CategoryType.DEPENSE).color("#8B5CF6").build();
        when(categoryRepository.existsByUserIdAndNameIgnoreCase(userId, "Voyage")).thenReturn(true);

        // Act & Assert
        assertThrows(ValidationException.class, () -> categoryService.create(userId, "STANDARD", request));
    }

    @Test
    @DisplayName("create - should save with isSystem false")
    void create_whenValidRequest_shouldSaveWithIsSystemFalse() {
        // Arrange
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("New Cat").type(CategoryType.DEPENSE).color("#000000").build();
        when(categoryRepository.existsByUserIdAndNameIgnoreCase(userId, "New Cat")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        categoryService.create(userId, "STANDARD", request);

        // Assert
        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().getIsSystem()).isFalse();
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("create - should check name case-insensitively")
    void create_nameShouldBeCaseInsensitiveDuplicateCheck() {
        // Arrange
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("VOYAGE").type(CategoryType.DEPENSE).color("#8B5CF6").build();
        when(categoryRepository.existsByUserIdAndNameIgnoreCase(userId, "VOYAGE")).thenReturn(true);

        // Act & Assert
        assertThrows(ValidationException.class, () -> categoryService.create(userId, "STANDARD", request));
    }

    // --- UPDATE ---

    @Test
    @DisplayName("update - should throw ResourceNotFoundException when category not found")
    void update_whenCategoryNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        UpdateCategoryRequest request = UpdateCategoryRequest.builder().name("Updated").build();
        when(categoryRepository.findByIdAndUserId(99L, userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> categoryService.update(userId, 99L, request));
    }

    @Test
    @DisplayName("update - should throw ValidationException when category is system")
    void update_whenCategoryIsSystem_shouldThrowValidationException() {
        // Arrange
        UpdateCategoryRequest request = UpdateCategoryRequest.builder().name("Updated").build();
        when(categoryRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(systemCat));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
                () -> categoryService.update(userId, 1L, request));
        assertThat(exception.getMessage()).contains("System categories cannot be modified");
    }

    @Test
    @DisplayName("update - should throw ValidationException when name conflicts with other category")
    void update_whenNameConflictsWithOtherCategory_shouldThrowValidationException() {
        // Arrange
        UpdateCategoryRequest request = UpdateCategoryRequest.builder().name("Existing").build();
        when(categoryRepository.findByIdAndUserId(5L, userId)).thenReturn(Optional.of(customCat));
        when(categoryRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, "Existing", 5L)).thenReturn(true);

        // Act & Assert
        assertThrows(ValidationException.class, () -> categoryService.update(userId, 5L, request));
    }

    @Test
    @DisplayName("update - should succeed when using same name as current category")
    void update_whenSameNameAsCurrentCategory_shouldSucceed() {
        // Arrange
        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .name("Voyage").type(CategoryType.DEPENSE).color("#8B5CF6").build();
        when(categoryRepository.findByIdAndUserId(5L, userId)).thenReturn(Optional.of(customCat));
        when(categoryRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, "Voyage", 5L)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(customCat);

        // Act
        categoryService.update(userId, 5L, request);

        // Assert
        verify(categoryRepository).save(any(Category.class));
    }

    // --- DELETE ---

    @Test
    @DisplayName("delete - should throw ValidationException when category is system")
    void delete_whenCategoryIsSystem_shouldThrowValidationException() {
        // Arrange
        when(categoryRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(systemCat));

        // Act & Assert
        assertThrows(ValidationException.class, () -> categoryService.delete(userId, 1L));
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete - should throw ValidationException when category has transactions")
    void delete_whenCategoryHasTransactions_shouldThrowValidationException() {
        // Arrange
        when(categoryRepository.findByIdAndUserId(5L, userId)).thenReturn(Optional.of(customCat));
        when(transactionRepository.existsByCategoryIdAndIsDeletedFalse(5L)).thenReturn(true);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
                () -> categoryService.delete(userId, 5L));
        assertThat(exception.getMessage()).contains("existing transactions");
    }

    @Test
    @DisplayName("delete - should throw ResourceNotFoundException when category not found")
    void delete_whenCategoryNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        when(categoryRepository.findByIdAndUserId(99L, userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> categoryService.delete(userId, 99L));
    }

    @Test
    @DisplayName("delete - should delete custom category successfully")
    void delete_whenValidCustomCategory_shouldDeleteSuccessfully() {
        // Arrange
        when(categoryRepository.findByIdAndUserId(5L, userId)).thenReturn(Optional.of(customCat));
        when(transactionRepository.existsByCategoryIdAndIsDeletedFalse(5L)).thenReturn(false);

        // Act
        categoryService.delete(userId, 5L);

        // Assert
        verify(categoryRepository).delete(customCat);
    }

    // --- LIST ---

    @Test
    @DisplayName("list - should return all categories for user ordered")
    void list_shouldReturnAllCategoriesForUser() {
        // Arrange
        when(categoryRepository.findByUserIdOrderByIsSystemDescNameAsc(userId))
                .thenReturn(List.of(systemCat, customCat));

        // Act
        List<CategoryResponse> result = categoryService.list(userId);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Alimentation");
        assertThat(result.get(0).getIsSystem()).isTrue();
        assertThat(result.get(1).getName()).isEqualTo("Voyage");
        assertThat(result.get(1).getIsSystem()).isFalse();
    }

    // --- EDGE CASES ---

    @Test
    @DisplayName("create - should succeed for ADMIN user even if limit is reached")
    void create_whenAdminUserExceedsLimit_shouldSucceed() {
        // Arrange
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("New Cat").type(CategoryType.DEPENSE).color("#000000").build();
        when(categoryRepository.existsByUserIdAndNameIgnoreCase(userId, "New Cat")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(customCat);

        // Act
        CategoryResponse result = categoryService.create(userId, "ADMIN", request);

        // Assert
        assertThat(result).isNotNull();
        verify(categoryRepository, never()).countByUserIdAndIsSystemFalseForUpdate(any());
    }

    /**
     * C-2 regression: Old code called existsByCategoryId which included soft-deleted
     * transactions, permanently blocking category deletion.
     *
     * Fixed behavior: when ALL transactions are soft-deleted
     * (existsByCategoryIdAndIsDeletedFalse returns false), the category CAN be deleted.
     *
     * This test would FAIL on the old code (old method returned true for soft-deleted
     * transactions, throwing ValidationException) and PASS after the C-2 fix.
     */
    @Test
    @DisplayName("C-2: delete - should ALLOW deletion when all transactions are soft-deleted")
    void delete_whenCategoryHasOnlySoftDeletedTransactions_shouldAllowDeletion() {
        // Arrange
        when(categoryRepository.findByIdAndUserId(5L, userId)).thenReturn(Optional.of(customCat));
        // C-2 fix: only non-deleted transactions are checked
        when(transactionRepository.existsByCategoryIdAndIsDeletedFalse(5L)).thenReturn(false); // all soft-deleted

        // Act & Assert — should NOT throw; category is deletable
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> categoryService.delete(userId, 5L));
        verify(categoryRepository).delete(customCat);
    }

    @Test
    @DisplayName("delete - should block deletion when active (non-deleted) transactions exist")
    void delete_whenCategoryHasActiveTransactions_shouldBlockDeletion() {
        // Arrange
        when(categoryRepository.findByIdAndUserId(5L, userId)).thenReturn(Optional.of(customCat));
        when(transactionRepository.existsByCategoryIdAndIsDeletedFalse(5L)).thenReturn(true); // active transactions exist

        // Act & Assert
        assertThrows(ValidationException.class, () -> categoryService.delete(userId, 5L));
    }
    // --- IDOR SECURITY ---

    @Test
    void update_userId2CannotUpdateUserId1CustomCategory() {
        // Arrange
        Long userId2 = 2L;
        UpdateCategoryRequest request = UpdateCategoryRequest.builder().name("Updated").build();
        when(categoryRepository.findByIdAndUserId(customCat.getId(), userId2)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> categoryService.update(userId2, customCat.getId(), request));
    }

    @Test
    void delete_userId2CannotDeleteUserId1CustomCategory() {
        // Arrange
        Long userId2 = 2L;
        when(categoryRepository.findByIdAndUserId(customCat.getId(), userId2)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> categoryService.delete(userId2, customCat.getId()));
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Explicit IDOR: User B cannot delete User A's category")
    void explicitIdorTest_userBCannotDeleteUserACategory() {
        Long userB = 2L;
        // The service scopes the query to userB's ID, so it won't find userA's category
        when(categoryRepository.findByIdAndUserId(customCat.getId(), userB)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoryService.delete(userB, customCat.getId()));
    }

    /**
     * I-2 regression: Verifies that the STANDARD user limit check uses the
     * locked count method (countByUserIdAndIsSystemFalseForUpdate), not the
     * unlocked one.  Old code called countByUserIdAndIsSystemFalse; after the
     * fix, countByUserIdAndIsSystemFalseForUpdate is called instead.
     *
     * This test would FAIL on the old service code (used the non-locking method)
     * and PASS after the I-2 fix.
     */
    @Test
    @DisplayName("I-2: create() for STANDARD user calls the PESSIMISTIC_WRITE-locked count, not the unlocked one")
    void create_standardUser_shouldCallLockedCount() {
        // Arrange
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("LockedCountCat").type(CategoryType.DEPENSE).color("#000000").build();
        when(categoryRepository.countByUserIdAndIsSystemFalseForUpdate(userId)).thenReturn(0L);
        when(categoryRepository.existsByUserIdAndNameIgnoreCase(userId, "LockedCountCat")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(customCat);

        // Act
        categoryService.create(userId, "STANDARD", request);

        // Assert
        verify(categoryRepository).countByUserIdAndIsSystemFalseForUpdate(userId);   // locked version called
        verify(categoryRepository, never()).countByUserIdAndIsSystemFalse(any());     // unlocked NOT called
    }

    /**
     * I-2 regression: DataIntegrityViolationException from a simultaneous insert that
     * sneaks past the lock (e.g. different category names at exact same count) must
     * propagate out of the service and reach GlobalExceptionHandler (maps it to 409).
     * The service must not silently swallow it.
     */
    @Test
    @DisplayName("I-2: DataIntegrityViolationException from concurrent insert propagates to GlobalExceptionHandler")
    void create_whenConcurrentInsertViolatesUniqueConstraint_shouldPropagateException() {
        // Arrange
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("RaceCat").type(CategoryType.DEPENSE).color("#000000").build();
        when(categoryRepository.countByUserIdAndIsSystemFalseForUpdate(userId)).thenReturn(0L);
        when(categoryRepository.existsByUserIdAndNameIgnoreCase(userId, "RaceCat")).thenReturn(false);
        when(categoryRepository.save(any(Category.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("unique_user_category"));

        // Act & Assert — must propagate, NOT be swallowed
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> categoryService.create(userId, "STANDARD", request));
    }
}
