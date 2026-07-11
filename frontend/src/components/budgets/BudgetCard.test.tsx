import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import { BudgetCard } from './BudgetCard';
import type { BudgetProgress } from '../../types';

const mockBudgetProgress: BudgetProgress = {
  budget: {
    id: 1,
    category: { 
      id: 10, 
      name: 'Alimentation', 
      color: '#EF4444', 
      type: 'DEPENSE', 
      isSystem: true 
    },
    budgetYear: 2026,
    budgetMonth: 5,
    limitAmount: 200000, // 2000 DH
    alertThreshold: 80
  },
  spentAmount: 100000, // 1000 DH
  remainingAmount: 100000, // 1000 DH
  spentPercentage: 50,
  alertStatus: 'NORMAL'
};

describe('BudgetCard', () => {
  const onEdit = vi.fn();
  const onDelete = vi.fn();

  it('BudgetCard_shouldDisplayCategoryName', () => {
    // Arrange
    render(
      <BudgetCard 
        progress={mockBudgetProgress} 
        onEdit={onEdit} 
        onDelete={onDelete} 
      />
    );

    // Assert
    expect(screen.getByText('Alimentation')).toBeInTheDocument();
  });

  it('BudgetCard_shouldDisplayNormalStatusWhenUnderThreshold', () => {
    // Arrange
    render(
      <BudgetCard 
        progress={mockBudgetProgress} 
        onEdit={onEdit} 
        onDelete={onDelete} 
      />
    );

    // Assert
    expect(screen.getByText('En cours')).toBeInTheDocument();
  });

  it('BudgetCard_shouldDisplayWarningStatusWhenApproachingLimit', () => {
    // Arrange
    const warningProgress: BudgetProgress = {
      ...mockBudgetProgress,
      alertStatus: 'WARNING',
      spentPercentage: 85
    };

    render(
      <BudgetCard 
        progress={warningProgress} 
        onEdit={onEdit} 
        onDelete={onDelete} 
      />
    );

    // Assert
    expect(screen.getByText(/Alerte > 80%/i)).toBeInTheDocument();
  });

  it('BudgetCard_shouldDisplayCriticalStatusWhenExceeded', () => {
    // Arrange
    const criticalProgress: BudgetProgress = {
      ...mockBudgetProgress,
      alertStatus: 'CRITICAL',
      spentPercentage: 110
    };

    render(
      <BudgetCard 
        progress={criticalProgress} 
        onEdit={onEdit} 
        onDelete={onDelete} 
      />
    );

    // Assert
    expect(screen.getByText('Dépassé')).toBeInTheDocument();
  });

  it('BudgetCard_shouldDisplaySpentAndRemainingAmounts', () => {
    // Arrange
    render(
      <BudgetCard 
        progress={mockBudgetProgress} 
        onEdit={onEdit} 
        onDelete={onDelete} 
      />
    );

    // Assert
    // spentAmount: 100000 -> 1 000,00 DH
    // limitAmount: 200000 -> 2 000,00 DH
    // remainingAmount: 100000 -> 1 000,00 DH
    expect(screen.getAllByText(/1.000,00/i).length).toBeGreaterThan(0);
    expect(screen.getByText(/2.000,00/i)).toBeInTheDocument();
  });

  it('BudgetCard_shouldApplyDangerBorderWhenCritical', () => {
    // Arrange
    const criticalProgress: BudgetProgress = {
      ...mockBudgetProgress,
      alertStatus: 'CRITICAL'
    };

    const { container } = render(
      <BudgetCard 
        progress={criticalProgress} 
        onEdit={onEdit} 
        onDelete={onDelete} 
      />
    );

    // Assert
    const card = container.firstChild;
    expect(card).toHaveClass('border-danger/30');
  });

  it('BudgetCard_editButton_shouldCallOnEdit', async () => {
    // Arrange
    const user = userEvent.setup();
    render(
      <BudgetCard 
        progress={mockBudgetProgress} 
        onEdit={onEdit} 
        onDelete={onDelete} 
      />
    );

    // Act
    const editButton = screen.getByTitle('Modifier');
    await user.click(editButton);

    // Assert
    expect(onEdit).toHaveBeenCalledWith(1);
  });

  it('BudgetCard_deleteButton_shouldCallOnDelete', async () => {
    // Arrange
    const user = userEvent.setup();
    render(
      <BudgetCard 
        progress={mockBudgetProgress} 
        onEdit={onEdit} 
        onDelete={onDelete} 
      />
    );

    // Act
    const deleteButton = screen.getByTitle('Supprimer');
    await user.click(deleteButton);

    // Assert
    expect(onDelete).toHaveBeenCalledWith(1);
  });

  it('BudgetCard_progressBar_shouldShowCorrectPercentage', () => {
    // Arrange
    render(
      <BudgetCard 
        progress={mockBudgetProgress} 
        onEdit={onEdit} 
        onDelete={onDelete} 
      />
    );

    // Assert
    expect(screen.getByText('50%')).toBeInTheDocument();
  });
});
