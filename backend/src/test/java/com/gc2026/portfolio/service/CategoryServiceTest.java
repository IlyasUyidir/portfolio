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
        when(categoryRepository.countByUserIdAndIsSystemFalse(userId)).thenReturn(10L);

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
        verify(categoryRepository, never()).countByUserIdAndIsSystemFalse(any());
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
        when(transactionRepository.existsByCategoryId(5L)).thenReturn(true);

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
        when(transactionRepository.existsByCategoryId(5L)).thenReturn(false);

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
        verify(categoryRepository, never()).countByUserIdAndIsSystemFalse(any());
    }

    @Test
    @DisplayName("delete - should block deletion even with soft-deleted transactions")
    void delete_whenCategoryHasSoftDeletedTransactions_shouldStillBlockDeletion() {
        // Arrange
        when(categoryRepository.findByIdAndUserId(5L, userId)).thenReturn(Optional.of(customCat));
        // existsByCategoryId in repository checks all transactions, including isDeleted=true/false
        // depending on how it's implemented. In this project, existsByCategoryId is a standard
        // Spring Data JPA method which doesn't automatically filter soft-deleted unless specified.
        when(transactionRepository.existsByCategoryId(5L)).thenReturn(true);

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
}
