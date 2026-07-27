package com.gc2026.portfolio.repository;

import com.gc2026.portfolio.domain.entity.Category;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    /**
     * I-2: The standard derived `countByUserIdAndIsSystemFalse` is replaced by a
     * PESSIMISTIC_WRITE-locked query.  This forces all concurrent "count then insert"
     * operations for the same user to queue behind a single DB lock, preventing two
     * threads both seeing count=9, both passing the guard, and both inserting a new
     * category (which would breach the STANDARD limit of 10).
     *
     * The lock is held on all existing custom-category rows for the user inside the
     * enclosing @Transactional in CategoryService.create().  When the transaction
     * commits (or rolls back), the next thread acquires the lock and re-counts.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT COUNT(c) FROM Category c WHERE c.userId = :userId AND c.isSystem = false")
    long countByUserIdAndIsSystemFalseForUpdate(@Param("userId") Long userId);

    /**
     * Non-locking count — used for read-only contexts (admin pages, metrics) where
     * serialisation isn't needed.
     */
    long countByUserIdAndIsSystemFalse(Long userId);
}
