import { renderHook, waitFor } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { useGoals } from './useGoals';
import * as goalApi from '../../api/goalApi';

vi.mock('../../api/goalApi', () => ({
  listGoals: vi.fn(),
  getGoalProgress: vi.fn(),
}));

describe('useGoals', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const mockGoal1 = { id: 1, name: 'Goal 1', targetAmount: 1000, currentAmount: 500 };
  const mockGoal2 = { id: 2, name: 'Goal 2', targetAmount: 2000, currentAmount: 1000 };
  const mockProgress1 = { goal: mockGoal1, currentAmount: 500, targetAmount: 1000, percentage: 50, milestones: [] };
  const mockProgress2 = { goal: mockGoal2, currentAmount: 1000, targetAmount: 2000, percentage: 50, milestones: [] };

  it('should return loading initially and then fetch goals and their progress on success', async () => {
    (goalApi.listGoals as any).mockResolvedValue([mockGoal1, mockGoal2]);
    (goalApi.getGoalProgress as any).mockImplementation((id: number) => {
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
    (goalApi.listGoals as any).mockResolvedValue([mockGoal1, mockGoal2]);
    (goalApi.getGoalProgress as any).mockImplementation((id: number) => {
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
    (goalApi.listGoals as any).mockRejectedValue(new Error('Goals error'));

    const { result } = renderHook(() => useGoals());

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.error).toBe('Goals error');
    expect(result.current.data).toBeNull();
  });
});
