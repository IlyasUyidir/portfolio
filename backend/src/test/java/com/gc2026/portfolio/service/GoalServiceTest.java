package com.gc2026.portfolio.service;

import com.gc2026.portfolio.domain.entity.Goal;
import com.gc2026.portfolio.domain.exception.ResourceNotFoundException;
import com.gc2026.portfolio.repository.GoalContributionRepository;
import com.gc2026.portfolio.repository.GoalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private GoalContributionRepository goalContributionRepository;

    @InjectMocks
    private GoalService goalService;

    @Test
    void deleteGoal_whenUserOwnsGoal_shouldDelete() {
        // Arrange
        Long userId = 1L;
        Long goalId = 100L;
        Goal goal = Goal.builder().id(goalId).userId(userId).build();

        when(goalRepository.findByIdAndUserId(goalId, userId)).thenReturn(Optional.of(goal));

        // Act
        goalService.deleteGoal(userId, goalId);

        // Assert
        verify(goalRepository, times(1)).delete(goal);
    }

    @Test
    void deleteGoal_whenUserDoesNotOwnGoal_shouldThrowException() {
        // Arrange
        Long userId = 1L;
        Long goalId = 100L;

        when(goalRepository.findByIdAndUserId(goalId, userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> goalService.deleteGoal(userId, goalId));
        verify(goalRepository, never()).delete(any());
    }
}
