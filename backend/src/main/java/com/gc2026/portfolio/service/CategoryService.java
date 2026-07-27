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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private static final int STANDARD_CATEGORY_LIMIT = 10;

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(Long userId) {
        return categoryRepository.findByUserIdOrderByIsSystemDescNameAsc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse create(Long userId, String userRole, CreateCategoryRequest dto) {
        // Limit enforcement: Standard users max 10 custom categories
        if ("STANDARD".equals(userRole)) {
            // I-2: Use the PESSIMISTIC_WRITE-locked count to serialise concurrent inserts
            // for the same user, preventing limit bypass via count-then-create race.
            long customCount = categoryRepository.countByUserIdAndIsSystemFalseForUpdate(userId);
            if (customCount >= STANDARD_CATEGORY_LIMIT) {
                throw new ValidationException(
                        "Standard users are limited to " + STANDARD_CATEGORY_LIMIT + " custom categories");
            }
        }

        // Duplicate name check (case-insensitive)
        if (categoryRepository.existsByUserIdAndNameIgnoreCase(userId, dto.getName())) {
            throw new ValidationException("Category name already exists");
        }

        Category category = Category.builder()
                .userId(userId)
                .name(dto.getName())
                .color(dto.getColor())
                .type(dto.getType())
                .isSystem(false)
                .build();

        category = categoryRepository.save(category);
        return toResponse(category);
    }

    @Transactional
    public CategoryResponse update(Long userId, Long catId, UpdateCategoryRequest dto) {
        Category category = categoryRepository.findByIdAndUserId(catId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        // System categories cannot be modified
        if (category.getIsSystem()) {
            throw new ValidationException("System categories cannot be modified");
        }

        // Duplicate name check excluding self (case-insensitive)
        if (categoryRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, dto.getName(), catId)) {
            throw new ValidationException("Category name already exists");
        }

        category.setName(dto.getName());
        category.setColor(dto.getColor());
        category.setType(dto.getType());

        category = categoryRepository.save(category);
        return toResponse(category);
    }

    @Transactional
    public void delete(Long userId, Long catId) {
        Category category = categoryRepository.findByIdAndUserId(catId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        // System categories cannot be deleted
        if (category.getIsSystem()) {
            throw new ValidationException("System categories cannot be deleted");
        }

        // C-2: Use existsByCategoryIdAndIsDeletedFalse (not existsByCategoryId) so that
        // categories whose transactions are all soft-deleted can be deleted correctly.
        if (transactionRepository.existsByCategoryIdAndIsDeletedFalse(catId)) {
            throw new ValidationException("Cannot delete category with existing transactions");
        }

        categoryRepository.delete(category);
    }

    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .color(category.getColor())
                .type(category.getType().name())
                .isSystem(category.getIsSystem())
                .build();
    }
}
