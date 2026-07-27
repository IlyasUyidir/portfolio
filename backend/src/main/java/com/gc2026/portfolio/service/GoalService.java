package com.gc2026.portfolio.service;

import com.gc2026.portfolio.domain.entity.Goal;
import com.gc2026.portfolio.domain.entity.GoalContribution;
import com.gc2026.portfolio.domain.enums.GoalStatus;
import com.gc2026.portfolio.domain.enums.UserRole;
import com.gc2026.portfolio.domain.exception.ResourceNotFoundException;
import com.gc2026.portfolio.domain.exception.ValidationException;
import com.gc2026.portfolio.dto.request.ContributeRequest;
import com.gc2026.portfolio.dto.request.CreateGoalRequest;
import com.gc2026.portfolio.dto.response.GoalProgressResponse;
import com.gc2026.portfolio.dto.response.GoalResponse;
import com.gc2026.portfolio.dto.response.MilestonesDto;
import com.gc2026.portfolio.repository.GoalContributionRepository;
import com.gc2026.portfolio.repository.GoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.persistence.OptimisticLockException;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final GoalContributionRepository goalContributionRepository;

    @Transactional
    public GoalResponse createGoal(Long userId, String userRole, CreateGoalRequest request) {
        if (UserRole.STANDARD.name().equals(userRole)) {
            long activeGoalsCount = goalRepository.countByUserIdAndStatusIn(userId, List.of(GoalStatus.EN_COURS, GoalStatus.EN_RETARD));
            if (activeGoalsCount >= 1) {
                throw new ValidationException("Standard users can only have 1 active goal");
            }
        }

        Goal goal = Goal.builder()
                .userId(userId)
                .title(request.getTitle())
                .targetAmount(request.getTargetAmount())
                .targetDate(request.getTargetDate())
                .build();

        goal = goalRepository.save(goal);
        return mapToResponse(goal);
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> getUserGoals(Long userId) {
        return goalRepository.findTop50ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public GoalResponse addContribution(Long userId, Long goalId, ContributeRequest request) {
        Goal goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));

        if (goal.getStatus() == GoalStatus.ATTEINT) {
            throw new ValidationException("Cannot contribute to an already achieved goal");
        }

        GoalContribution contribution = GoalContribution.builder()
                .goal(goal)
                .amount(request.getAmount())
                .contributionDate(LocalDate.now())
                .build();

        goalContributionRepository.save(contribution);

        goal.setCurrentAmount(goal.getCurrentAmount() + request.getAmount());

        if (goal.getCurrentAmount() >= goal.getTargetAmount()) {
            goal.setStatus(GoalStatus.ATTEINT);
        } else if (LocalDate.now().isAfter(goal.getTargetDate())) {
            goal.setStatus(GoalStatus.EN_RETARD);
        } else {
            goal.setStatus(GoalStatus.EN_COURS);
        }

        try {
            goal = goalRepository.save(goal);
        } catch (OptimisticLockException ex) {
            // C-1: Two concurrent contributions read the same @Version value.
            // Surface a clean 409 rather than a 500. The client should retry
            // the contribution — we do not retry here because contributions are
            // not idempotent and the client is better positioned to decide.
            throw new ValidationException(
                    "Goal was modified concurrently — please retry your contribution");
        }
        return mapToResponse(goal);
    }

    @Transactional(readOnly = true)
    public GoalProgressResponse getProgress(Long userId, Long goalId) {
        Goal goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));

        int progressPercentage = 0;
        if (goal.getTargetAmount() > 0) {
            progressPercentage = (int) Math.round((double) goal.getCurrentAmount() / goal.getTargetAmount() * 100);
        }

        MilestonesDto milestones = MilestonesDto.builder()
                .twentyFive(progressPercentage >= 25)
                .fifty(progressPercentage >= 50)
                .seventyFive(progressPercentage >= 75)
                .hundred(progressPercentage >= 100)
                .build();

        return GoalProgressResponse.builder()
                .goal(mapToResponse(goal))
                .progressPercentage(progressPercentage)
                .milestones(milestones)
                .build();
    }

    @Transactional
    public void deleteGoal(Long userId, Long id) {
        Goal goal = goalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
        goalRepository.delete(goal);
    }

    private GoalResponse mapToResponse(Goal goal) {
        return GoalResponse.builder()
                .id(goal.getId())
                .userId(goal.getUserId())
                .title(goal.getTitle())
                .targetAmount(goal.getTargetAmount())
                .currentAmount(goal.getCurrentAmount())
                .targetDate(goal.getTargetDate())
                .status(goal.getStatus().name())
                .createdAt(goal.getCreatedAt())
                .build();
    }
}
