import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { GoalCard } from './GoalCard';
import type { Goal, GoalProgress } from '../../types';

describe('GoalCard', () => {
  const mockGoal: Goal = {
    id: 1,
    title: 'Voiture',
    targetAmount: 5000,
    currentAmount: 1250,
    targetDate: '2027-01-01',
    status: 'EN_COURS',
    createdAt: '2026-01-01'
  };

  const mockProgress: GoalProgress = {
    goalId: 1,
    title: 'Voiture',
    targetAmount: 5000,
    currentAmount: 1250,
    progressPercentage: 25,
    milestones: {
      twentyFive: true,
      fifty: false,
      seventyFive: false,
      hundred: false
    },
    status: 'EN_COURS'
  };

  const defaultProps = {
    goal: mockGoal,
    progress: mockProgress,
    onContribute: vi.fn(),
    onDelete: vi.fn(),
  };

  it('GoalCard_shouldDisplayGoalTitle', () => {
    // Arrange
    render(<GoalCard {...defaultProps} />);

    // Act & Assert
    expect(screen.getByText('Voiture')).toBeInTheDocument();
  });

  it('GoalCard_shouldDisplayCorrectStatusChip', () => {
    // Arrange
    render(<GoalCard {...defaultProps} />);

    // Act & Assert
    expect(screen.getByText('EN COURS')).toBeInTheDocument();
  });

  it('GoalCard_shouldDisplayAtteintChipWhenAchieved', () => {
    // Arrange
    const achievedGoal = { ...mockGoal, status: 'ATTEINT' as const };
    render(<GoalCard {...defaultProps} goal={achievedGoal} />);

    // Act & Assert
    expect(screen.getByText('ATTEINT')).toBeInTheDocument();
  });

  it('GoalCard_shouldDisplayProgressPercentage', () => {
    // Arrange
    render(<GoalCard {...defaultProps} />);

    // Act & Assert
    expect(screen.getByText('25%')).toBeInTheDocument();
  });

  it('GoalCard_shouldHighlight25PercentMilestone', () => {
    // Arrange
    render(<GoalCard {...defaultProps} />);

    // Act & Assert
    // The milestone dot for 25% has reached=true, so it should have bg-primary
    // Looking at GoalCard.tsx, the title attribute of the milestone dot is `${m.value}%`
    const milestoneDot = screen.getByTitle('25%');
    expect(milestoneDot).toHaveClass('bg-primary');
  });

  it('GoalCard_shouldNotShowContributeButtonWhenAchieved', () => {
    // Arrange
    const achievedGoal = { ...mockGoal, status: 'ATTEINT' as const };
    render(<GoalCard {...defaultProps} goal={achievedGoal} />);

    // Act & Assert
    expect(screen.queryByText('Contribuer')).not.toBeInTheDocument();
  });

  it('GoalCard_shouldShowContributeButtonWhenActive', () => {
    // Arrange
    render(<GoalCard {...defaultProps} />);

    // Act & Assert
    expect(screen.getByText('Contribuer')).toBeInTheDocument();
  });

  it('GoalCard_contributeButton_shouldCallOnContribute', async () => {
    // Arrange
    const user = userEvent.setup();
    const onContribute = vi.fn();
    render(<GoalCard {...defaultProps} onContribute={onContribute} />);

    // Act
    await user.click(screen.getByText('Contribuer'));

    // Assert
    expect(onContribute).toHaveBeenCalledWith(1, 'Voiture');
  });

  it('GoalCard_deleteButton_shouldShowConfirmDialog', async () => {
    // Arrange
    const user = userEvent.setup();
    render(<GoalCard {...defaultProps} />);

    // Act
    const deleteButton = screen.getByTitle("Supprimer l'objectif");
    await user.click(deleteButton);

    // Assert
    expect(screen.getByText(/Êtes-vous sûr de vouloir supprimer l'objectif/)).toBeInTheDocument();
  });

  it('GoalCard_confirmDelete_shouldCallOnDelete', async () => {
    // Arrange
    const user = userEvent.setup();
    const onDelete = vi.fn();
    render(<GoalCard {...defaultProps} onDelete={onDelete} />);

    // Act
    const deleteButton = screen.getByTitle("Supprimer l'objectif");
    await user.click(deleteButton);
    await user.click(screen.getByText('Confirmer'));

    // Assert
    expect(onDelete).toHaveBeenCalledWith(1);
  });

  it('GoalCard_cancelDelete_shouldNotCallOnDelete', async () => {
    // Arrange
    const user = userEvent.setup();
    const onDelete = vi.fn();
    render(<GoalCard {...defaultProps} onDelete={onDelete} />);

    // Act
    const deleteButton = screen.getByTitle("Supprimer l'objectif");
    await user.click(deleteButton);
    await user.click(screen.getByText('Annuler'));

    // Assert
    expect(onDelete).not.toHaveBeenCalled();
  });

  it('GoalCard_milestones_shouldReadFromCorrectKeyNames', () => {
    // Arrange
    // Contract check: ensure we are using camelCase keys (twentyFive, etc.)
    const progressWithCamelCase: GoalProgress = {
      ...mockProgress,
      milestones: {
        twentyFive: true,
        fifty: false,
        seventyFive: false,
        hundred: false
      }
    };
    render(<GoalCard {...defaultProps} progress={progressWithCamelCase} />);

    // Act & Assert
    const milestoneDot = screen.getByTitle('25%');
    expect(milestoneDot).toHaveClass('bg-primary');
  });
});
