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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private GoalContributionRepository goalContributionRepository;

    @InjectMocks
    private GoalService goalService;

    private Long userId;
    private Goal activeGoal;
    private Goal achievedGoal;

    @BeforeEach
    void setUp() {
        userId = 1L;
        activeGoal = Goal.builder()
                .id(1L)
                .userId(userId)
                .title("Voiture")
                .targetAmount(500000L)
                .currentAmount(100000L)
                .targetDate(LocalDate.now().plusMonths(6))
                .status(GoalStatus.EN_COURS)
                .build();

        achievedGoal = Goal.builder()
                .id(2L)
                .userId(userId)
                .title("Vacances")
                .targetAmount(200000L)
                .currentAmount(200000L)
                .targetDate(LocalDate.now().plusMonths(1))
                .status(GoalStatus.ATTEINT)
                .build();
    }

    @Nested
    @DisplayName("Create Goal Tests")
    class CreateGoalTests {

        @Test
        void createGoal_whenStandardUserAlreadyHasOneActiveGoal_shouldThrowValidationException() {
            // Arrange
            String userRole = UserRole.STANDARD.name();
            CreateGoalRequest request = new CreateGoalRequest();
            request.setTitle("New Goal");
            request.setTargetAmount(100000L);
            request.setTargetDate(LocalDate.now().plusMonths(3));
            when(goalRepository.countByUserIdAndStatusIn(eq(userId), any())).thenReturn(1L);

            // Act & Assert
            ValidationException exception = assertThrows(ValidationException.class, 
                () -> goalService.createGoal(userId, userRole, request));
            
            assertThat(exception.getMessage()).contains("1 active goal");
            verify(goalRepository, never()).save(any());
        }

        @Test
        void createGoal_whenStandardUserHasNoActiveGoals_shouldSucceed() {
            // Arrange
            String userRole = UserRole.STANDARD.name();
            CreateGoalRequest request = new CreateGoalRequest();
            request.setTitle("New Goal");
            request.setTargetAmount(100000L);
            request.setTargetDate(LocalDate.now().plusMonths(3));
            when(goalRepository.countByUserIdAndStatusIn(eq(userId), any())).thenReturn(0L);
            when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            GoalResponse response = goalService.createGoal(userId, userRole, request);

            // Assert
            assertThat(response).isNotNull();
            verify(goalRepository).save(any(Goal.class));
        }

        @Test
        void createGoal_whenPremiumUserHasMultipleActiveGoals_shouldSucceed() {
            // Arrange
            String userRole = UserRole.PREMIUM.name();
            CreateGoalRequest request = new CreateGoalRequest();
            request.setTitle("New Goal");
            request.setTargetAmount(100000L);
            request.setTargetDate(LocalDate.now().plusMonths(3));
            when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            goalService.createGoal(userId, userRole, request);

            // Assert
            verify(goalRepository, never()).countByUserIdAndStatusIn(any(), any());
            verify(goalRepository).save(any(Goal.class));
        }

        @Test
        void createGoal_shouldSetStatusToEnCoursByDefault() {
            // Arrange
            String userRole = UserRole.PREMIUM.name();
            CreateGoalRequest request = new CreateGoalRequest();
            request.setTitle("New Goal");
            request.setTargetAmount(100000L);
            request.setTargetDate(LocalDate.now().plusMonths(3));
            ArgumentCaptor<Goal> captor = ArgumentCaptor.forClass(Goal.class);
            when(goalRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            goalService.createGoal(userId, userRole, request);

            // Assert
            assertThat(captor.getValue().getStatus()).isEqualTo(GoalStatus.EN_COURS);
        }

        @Test
        void createGoal_shouldSetCurrentAmountToZero() {
            // Arrange
            String userRole = UserRole.PREMIUM.name();
            CreateGoalRequest request = new CreateGoalRequest();
            request.setTitle("New Goal");
            request.setTargetAmount(100000L);
            request.setTargetDate(LocalDate.now().plusMonths(3));
            ArgumentCaptor<Goal> captor = ArgumentCaptor.forClass(Goal.class);
            when(goalRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            goalService.createGoal(userId, userRole, request);

            // Assert
            assertThat(captor.getValue().getCurrentAmount()).isEqualTo(0L);
        }

        @Test
        void createGoal_standardUserWithEnRetardGoal_shouldCountAgainstLimit() {
            // Arrange
            String userRole = UserRole.STANDARD.name();
            CreateGoalRequest request = new CreateGoalRequest();
            request.setTitle("New Goal");
            request.setTargetAmount(100000L);
            request.setTargetDate(LocalDate.now().plusMonths(3));
            
            // ArgumentCaptor to verify the list of statuses passed to repository
            ArgumentCaptor<List<GoalStatus>> statusCaptor = ArgumentCaptor.forClass(List.class);
            when(goalRepository.countByUserIdAndStatusIn(eq(userId), statusCaptor.capture())).thenReturn(1L);

            // Act & Assert
            assertThrows(ValidationException.class, () -> goalService.createGoal(userId, userRole, request));
            
            assertThat(statusCaptor.getValue()).containsExactlyInAnyOrder(GoalStatus.EN_COURS, GoalStatus.EN_RETARD);
        }
    }

    @Nested
    @DisplayName("Add Contribution Tests")
    class AddContributionTests {

        @Test
        void addContribution_whenGoalNotFound_shouldThrowResourceNotFoundException() {
            // Arrange
            ContributeRequest request = new ContributeRequest();
            request.setAmount(50000L);
            when(goalRepository.findByIdAndUserId(99L, userId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class, () -> goalService.addContribution(userId, 99L, request));
        }

        @Test
        void addContribution_whenGoalAlreadyAchieved_shouldThrowValidationException() {
            // Arrange
            ContributeRequest request = new ContributeRequest();
            request.setAmount(50000L);
            when(goalRepository.findByIdAndUserId(achievedGoal.getId(), userId)).thenReturn(Optional.of(achievedGoal));

            // Act & Assert
            ValidationException exception = assertThrows(ValidationException.class, 
                () -> goalService.addContribution(userId, achievedGoal.getId(), request));
            assertThat(exception.getMessage()).contains("already achieved");
        }

        @Test
        void addContribution_shouldIncreaseCurrentAmount() {
            // Arrange
            ContributeRequest request = new ContributeRequest();
            request.setAmount(50000L);
            long initialAmount = activeGoal.getCurrentAmount();
            when(goalRepository.findByIdAndUserId(activeGoal.getId(), userId)).thenReturn(Optional.of(activeGoal));
            when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            GoalResponse response = goalService.addContribution(userId, activeGoal.getId(), request);

            // Assert
            assertThat(response.getCurrentAmount()).isEqualTo(initialAmount + 50000L);
            verify(goalRepository).save(activeGoal);
        }

        @Test
        void addContribution_whenContributionReachesTarget_shouldSetStatusToAtteint() {
            // Arrange
            activeGoal.setCurrentAmount(450000L);
            ContributeRequest request = new ContributeRequest();
            request.setAmount(50000L);
            when(goalRepository.findByIdAndUserId(activeGoal.getId(), userId)).thenReturn(Optional.of(activeGoal));
            when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            GoalResponse response = goalService.addContribution(userId, activeGoal.getId(), request);

            // Assert
            assertThat(response.getStatus()).isEqualTo(GoalStatus.ATTEINT.name());
        }

        @Test
        void addContribution_whenContributionExceedsTarget_shouldStillSetStatusToAtteint() {
            // Arrange
            activeGoal.setCurrentAmount(450000L);
            ContributeRequest request = new ContributeRequest();
            request.setAmount(100000L);
            when(goalRepository.findByIdAndUserId(activeGoal.getId(), userId)).thenReturn(Optional.of(activeGoal));
            when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            GoalResponse response = goalService.addContribution(userId, activeGoal.getId(), request);

            // Assert
            assertThat(response.getStatus()).isEqualTo(GoalStatus.ATTEINT.name());
            assertThat(response.getCurrentAmount()).isEqualTo(550000L);
        }

        @Test
        void addContribution_whenPastTargetDate_shouldSetStatusToEnRetard() {
            // Arrange
            activeGoal.setTargetDate(LocalDate.now().minusDays(1));
            ContributeRequest request = new ContributeRequest();
            request.setAmount(10000L);
            when(goalRepository.findByIdAndUserId(activeGoal.getId(), userId)).thenReturn(Optional.of(activeGoal));
            when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            GoalResponse response = goalService.addContribution(userId, activeGoal.getId(), request);

            // Assert
            assertThat(response.getStatus()).isEqualTo(GoalStatus.EN_RETARD.name());
        }

        @Test
        void addContribution_shouldSaveGoalContributionRecord() {
            // Arrange
            ContributeRequest request = new ContributeRequest();
            request.setAmount(50000L);
            when(goalRepository.findByIdAndUserId(activeGoal.getId(), userId)).thenReturn(Optional.of(activeGoal));
            when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            goalService.addContribution(userId, activeGoal.getId(), request);

            // Assert
            ArgumentCaptor<GoalContribution> captor = ArgumentCaptor.forClass(GoalContribution.class);
            verify(goalContributionRepository).save(captor.capture());
            assertThat(captor.getValue().getAmount()).isEqualTo(50000L);
            assertThat(captor.getValue().getGoal()).isEqualTo(activeGoal);
            assertThat(captor.getValue().getContributionDate()).isEqualTo(LocalDate.now());
        }
    }

    @Nested
    @DisplayName("Get Progress Tests")
    class GetProgressTests {

        @Test
        void getProgress_shouldCalculateProgressPercentageCorrectly() {
            // Arrange
            activeGoal.setCurrentAmount(250000L); // 50% of 500000
            when(goalRepository.findByIdAndUserId(activeGoal.getId(), userId)).thenReturn(Optional.of(activeGoal));

            // Act
            GoalProgressResponse response = goalService.getProgress(userId, activeGoal.getId());

            // Assert
            assertThat(response.getProgressPercentage()).isEqualTo(50);
        }

        @Test
        void getProgress_whenAt25Percent_twentyFiveMilestoneShouldBeTrue() {
            // Arrange
            // Exact boundary: 125,000 / 500,000 = 25%
            activeGoal.setCurrentAmount(125000L);
            when(goalRepository.findByIdAndUserId(activeGoal.getId(), userId)).thenReturn(Optional.of(activeGoal));

            // Act
            GoalProgressResponse response = goalService.getProgress(userId, activeGoal.getId());

            // Assert
            assertThat(response.getMilestones().isTwentyFive()).isTrue();
            assertThat(response.getMilestones().isFifty()).isFalse();
        }

        @Test
        void getProgress_whenJustBelow25Percent_roundingShouldStillWork() {
            // Arrange
            // 124,750 / 500,000 = 0.2495 -> 24.95% -> rounded to 25% by Math.round
            activeGoal.setCurrentAmount(124750L);
            when(goalRepository.findByIdAndUserId(activeGoal.getId(), userId)).thenReturn(Optional.of(activeGoal));

            // Act
            GoalProgressResponse response = goalService.getProgress(userId, activeGoal.getId());

            // Assert
            assertThat(response.getMilestones().isTwentyFive()).isTrue();
        }

        @Test
        void getProgress_whenAt100Percent_allMilestonesShouldBeTrue() {
            // Arrange
            activeGoal.setCurrentAmount(500000L);
            when(goalRepository.findByIdAndUserId(activeGoal.getId(), userId)).thenReturn(Optional.of(activeGoal));

            // Act
            GoalProgressResponse response = goalService.getProgress(userId, activeGoal.getId());

            // Assert
            assertThat(response.getMilestones().isTwentyFive()).isTrue();
            assertThat(response.getMilestones().isFifty()).isTrue();
            assertThat(response.getMilestones().isSeventyFive()).isTrue();
            assertThat(response.getMilestones().isHundred()).isTrue();
        }

        @Test
        void getProgress_whenGoalNotFound_shouldThrowResourceNotFoundException() {
            // Arrange
            when(goalRepository.findByIdAndUserId(99L, userId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class, () -> goalService.getProgress(userId, 99L));
        }

        @Test
        void getProgress_whenTargetAmountIsZero_shouldNotDivideByZero() {
            // Arrange
            activeGoal.setTargetAmount(0L);
            when(goalRepository.findByIdAndUserId(activeGoal.getId(), userId)).thenReturn(Optional.of(activeGoal));

            // Act
            GoalProgressResponse response = goalService.getProgress(userId, activeGoal.getId());

            // Assert
            assertThat(response.getProgressPercentage()).isEqualTo(0);
            assertThat(response.getMilestones().isHundred()).isFalse();
        }
    }

    @Nested
    @DisplayName("Delete Goal Tests")
    class DeleteGoalTests {

        @Test
        void deleteGoal_whenGoalBelongsToUser_shouldDelete() {
            // Arrange
            when(goalRepository.findByIdAndUserId(activeGoal.getId(), userId)).thenReturn(Optional.of(activeGoal));

            // Act
            goalService.deleteGoal(userId, activeGoal.getId());

            // Assert
            verify(goalRepository).delete(activeGoal);
        }

        @Test
        void deleteGoal_whenGoalBelongsToAnotherUser_shouldThrowResourceNotFoundException() {
            // Arrange
            when(goalRepository.findByIdAndUserId(activeGoal.getId(), userId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class, () -> goalService.deleteGoal(userId, activeGoal.getId()));
            verify(goalRepository, never()).delete(any());
        }

        @Test
        void deleteGoal_userId2CannotDeleteUserId1Goal() {
            // Arrange
            Long userId2 = 2L;
            when(goalRepository.findByIdAndUserId(activeGoal.getId(), userId2)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class, () -> goalService.deleteGoal(userId2, activeGoal.getId()));
            verify(goalRepository, never()).delete(any());
        }

        @Test
        void addContribution_userId2CannotContributeToUserId1Goal() {
            // Arrange
            Long userId2 = 2L;
            ContributeRequest request = new ContributeRequest();
            request.setAmount(50000L);
            when(goalRepository.findByIdAndUserId(activeGoal.getId(), userId2)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class, () -> goalService.addContribution(userId2, activeGoal.getId(), request));
        }

        @Test
        void getProgress_userId2CannotReadUserId1GoalProgress() {
            // Arrange
            Long userId2 = 2L;
            when(goalRepository.findByIdAndUserId(activeGoal.getId(), userId2)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class, () -> goalService.getProgress(userId2, activeGoal.getId()));
        }

        @Test
        @DisplayName("Explicit IDOR: User B cannot delete User A's goal")
        void explicitIdorTest_userBCannotDeleteUserAGoal() {
            Long userB = 2L;
            // The service scopes the query to userB's ID, so it won't find userA's goal
            when(goalRepository.findByIdAndUserId(activeGoal.getId(), userB)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> goalService.deleteGoal(userB, activeGoal.getId()));
        }
    }
}
