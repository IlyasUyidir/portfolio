package com.gc2026.portfolio.repository;

import com.gc2026.portfolio.domain.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserIdOrderByIsSystemDescNameAsc(Long userId);

    Optional<Category> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT c FROM Category c WHERE c.id = :id AND (c.userId = :userId OR c.isSystem = true)")
    Optional<Category> findByIdAndUserIdOrSystem(@Param("id") Long id, @Param("userId") Long userId);

    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(Long userId, String name, Long id);

    long countByUserIdAndIsSystemFalse(Long userId);
}
