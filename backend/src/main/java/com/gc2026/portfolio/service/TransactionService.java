package com.gc2026.portfolio.service;

import com.gc2026.portfolio.domain.entity.Category;
import com.gc2026.portfolio.domain.entity.Transaction;
import com.gc2026.portfolio.domain.enums.TransactionType;
import com.gc2026.portfolio.domain.exception.ResourceNotFoundException;
import com.gc2026.portfolio.dto.request.CreateTransactionRequest;
import com.gc2026.portfolio.dto.request.UpdateTransactionRequest;
import com.gc2026.portfolio.dto.response.CategoryResponse;
import com.gc2026.portfolio.dto.response.TransactionResponse;
import com.gc2026.portfolio.repository.CategoryRepository;
import com.gc2026.portfolio.repository.TransactionRepository;
import com.gc2026.portfolio.repository.TransactionSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public TransactionResponse create(Long userId, CreateTransactionRequest dto) {
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Transaction transaction = Transaction.builder()
                .userId(userId)
                .title(dto.getTitle())
                .amount(dto.getAmount())
                .type(dto.getType())
                .category(category)
                .txDate(dto.getTxDate())
                .description(dto.getDescription())
                .isDeleted(false)
                .build();

        transaction = transactionRepository.save(transaction);
        return toResponse(transaction);
    }

    @Transactional
    public TransactionResponse update(Long userId, Long txId, UpdateTransactionRequest dto) {
        Transaction transaction = transactionRepository.findByIdAndUserIdAndIsDeletedFalse(txId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        transaction.setTitle(dto.getTitle());
        transaction.setAmount(dto.getAmount());
        transaction.setType(dto.getType());
        transaction.setCategory(category);
        transaction.setTxDate(dto.getTxDate());
        transaction.setDescription(dto.getDescription());

        transaction = transactionRepository.save(transaction);
        return toResponse(transaction);
    }

    @Transactional
    public void delete(Long userId, Long txId) {
        Transaction transaction = transactionRepository.findByIdAndUserIdAndIsDeletedFalse(txId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        transaction.setIsDeleted(true);
        transactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(Long userId, Long txId) {
        Transaction transaction = transactionRepository.findByIdAndUserIdAndIsDeletedFalse(txId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        return toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public com.gc2026.portfolio.dto.response.PaginatedResponse<TransactionResponse> list(Long userId,
                                          LocalDate startDate,
                                          LocalDate endDate,
                                          TransactionType type,
                                          Long categoryId,
                                          String keyword,
                                          Pageable pageable) {

        Specification<Transaction> spec = TransactionSpecification.buildFilter(
                userId, startDate, endDate, type, categoryId, keyword);

        Page<TransactionResponse> page = transactionRepository.findAll(spec, pageable).map(this::toResponse);
        return com.gc2026.portfolio.dto.response.PaginatedResponse.from(page);
    }

    private TransactionResponse toResponse(Transaction tx) {
        CategoryResponse categoryResponse = CategoryResponse.builder()
                .id(tx.getCategory().getId())
                .name(tx.getCategory().getName())
                .color(tx.getCategory().getColor())
                .type(tx.getCategory().getType().name())
                .isSystem(tx.getCategory().getIsSystem())
                .build();

        return TransactionResponse.builder()
                .id(tx.getId())
                .title(tx.getTitle())
                .amount(tx.getAmount())
                .type(tx.getType().name())
                .category(categoryResponse)
                .txDate(tx.getTxDate())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
