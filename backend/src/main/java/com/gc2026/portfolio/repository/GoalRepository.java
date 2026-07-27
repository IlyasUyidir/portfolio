package com.gc2026.portfolio.repository;

import com.gc2026.portfolio.domain.entity.Goal;
import com.gc2026.portfolio.domain.enums.GoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {

    List<Goal> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Goal> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndStatusIn(Long userId, List<GoalStatus> statuses);
}
