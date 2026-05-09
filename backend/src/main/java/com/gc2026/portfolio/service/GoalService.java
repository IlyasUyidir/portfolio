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
        return goalRepository.findByUserId(userId)
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

        goal = goalRepository.save(goal);
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

        Map<Integer, Boolean> milestones = new HashMap<>();
        milestones.put(25, progressPercentage >= 25);
        milestones.put(50, progressPercentage >= 50);
        milestones.put(75, progressPercentage >= 75);
        milestones.put(100, progressPercentage >= 100);

        return GoalProgressResponse.builder()
                .goal(mapToResponse(goal))
                .progressPercentage(progressPercentage)
                .milestones(milestones)
                .build();
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
