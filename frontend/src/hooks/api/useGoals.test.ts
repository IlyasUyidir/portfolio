import { renderHook, waitFor } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { useGoals } from './useGoals';
import * as goalApi from '../../api/goalApi';
import type { Goal, GoalProgress } from '../../types';

vi.mock('../../api/goalApi', () => ({
  listGoals: vi.fn(),
  getGoalProgress: vi.fn(),
}));

describe('useGoals', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // Goal requires: id, title, targetAmount, currentAmount, targetDate, status, createdAt
  const mockGoal1: Goal = {
    id: 1,
    title: 'Goal 1',
    targetAmount: 100000,
    currentAmount: 50000,
    targetDate: '2027-01-01',
    status: 'EN_COURS',
    createdAt: '2026-01-01T00:00:00Z',
  };
  const mockGoal2: Goal = {
    id: 2,
    title: 'Goal 2',
    targetAmount: 200000,
    currentAmount: 100000,
    targetDate: '2027-06-01',
    status: 'EN_COURS',
    createdAt: '2026-01-01T00:00:00Z',
  };

  // GoalProgress requires: goalId, title, targetAmount, currentAmount, progressPercentage,
  // milestones: {twentyFive, fifty, seventyFive, hundred}, status
  const mockProgress1: GoalProgress = {
    goalId: 1,
    title: 'Goal 1',
    targetAmount: 100000,
    currentAmount: 50000,
    progressPercentage: 50,
    milestones: { twentyFive: true, fifty: true, seventyFive: false, hundred: false },
    status: 'EN_COURS',
  };
  const mockProgress2: GoalProgress = {
    goalId: 2,
    title: 'Goal 2',
    targetAmount: 200000,
    currentAmount: 100000,
    progressPercentage: 50,
    milestones: { twentyFive: true, fifty: true, seventyFive: false, hundred: false },
    status: 'EN_COURS',
  };

  it('should return loading initially and then fetch goals and their progress on success', async () => {
    vi.mocked(goalApi.listGoals).mockResolvedValue([mockGoal1, mockGoal2]);
    vi.mocked(goalApi.getGoalProgress).mockImplementation((id: number) => {
      if (id === 1) return Promise.resolve(mockProgress1);
      if (id === 2) return Promise.resolve(mockProgress2);
      return Promise.reject(new Error('Not found'));
    });

    const { result } = renderHook(() => useGoals());

    expect(result.current.isLoading).toBe(true);

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.data?.goals).toEqual([mockGoal1, mockGoal2]);
    expect(result.current.data?.progressMap[1]).toEqual(mockProgress1);
    expect(result.current.data?.progressMap[2]).toEqual(mockProgress2);
    expect(result.current.data?.failedProgressGoalIds).toEqual([]);
    expect(result.current.error).toBeNull();
  });

  it('should handle partial failure of getGoalProgress correctly', async () => {
    vi.mocked(goalApi.listGoals).mockResolvedValue([mockGoal1, mockGoal2]);
    vi.mocked(goalApi.getGoalProgress).mockImplementation((id: number) => {
      if (id === 1) return Promise.resolve(mockProgress1);
      if (id === 2) return Promise.reject(new Error('Progress failed'));
      return Promise.reject(new Error('Not found'));
    });

    const { result } = renderHook(() => useGoals());

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.data?.goals).toEqual([mockGoal1, mockGoal2]);
    expect(result.current.data?.progressMap[1]).toEqual(mockProgress1);
    expect(result.current.data?.progressMap[2]).toBeUndefined();
    expect(result.current.data?.failedProgressGoalIds).toEqual([2]);
    expect(result.current.error).toBeNull();
  });

  it('should return error if listGoals fails', async () => {
    vi.mocked(goalApi.listGoals).mockRejectedValue(new Error('Goals error'));

    const { result } = renderHook(() => useGoals());

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.error).toBe('Goals error');
    expect(result.current.data).toBeNull();
  });
});
