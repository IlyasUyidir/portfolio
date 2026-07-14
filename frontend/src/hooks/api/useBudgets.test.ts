import { renderHook, waitFor } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { useBudgets } from './useBudgets';
import * as budgetApi from '../../api/budgetApi';
import type { BudgetProgress } from '../../types';

vi.mock('../../api/budgetApi', () => ({
  listBudgetsByMonth: vi.fn(),
}));

describe('useBudgets', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const mockBudgetProgress: BudgetProgress = {
    budget: {
      id: 1, limitAmount: 50000, budgetMonth: 5, budgetYear: 2026, alertThreshold: 80,
      category: { id: 2, name: 'Food', color: '#f00', type: 'DEPENSE', isSystem: true }
    },
    spentAmount: 10000, remainingAmount: 40000, spentPercentage: 20, alertStatus: 'NORMAL'
  };

  it('should return loading initially and then data on success', async () => {
    (budgetApi.listBudgetsByMonth as any).mockResolvedValue([mockBudgetProgress]);

    const { result } = renderHook(() => useBudgets('2026-05'));

    expect(result.current.isLoading).toBe(true);
    expect(result.current.data).toBeNull();

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.data).toEqual([mockBudgetProgress]);
    expect(result.current.error).toBeNull();
    expect(budgetApi.listBudgetsByMonth).toHaveBeenCalledWith('2026-05');
  });

  it('should return error if API call fails', async () => {
    (budgetApi.listBudgetsByMonth as any).mockRejectedValue(new Error('Network error'));

    const { result } = renderHook(() => useBudgets('2026-05'));

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.error).toBe('Network error');
    expect(result.current.data).toBeNull();
  });
});
