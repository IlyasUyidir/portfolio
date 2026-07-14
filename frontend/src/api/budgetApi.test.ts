import { describe, it, expect, vi, beforeEach } from 'vitest';
import apiClient from './apiClient';
import { createBudget, listBudgetsByMonth, getBudgetProgress, deleteBudget } from './budgetApi';
import type { Budget, BudgetProgress, CreateBudgetRequest } from '../types';

vi.mock('./apiClient', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
    delete: vi.fn(),
  }
}));

describe('budgetApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const mockBudget: Budget = {
    id: 1,
    limitAmount: 50000,
    budgetMonth: 5,
    budgetYear: 2026,
    alertThreshold: 80,
    category: { id: 2, name: 'Food', color: '#ff0000', type: 'DEPENSE', isSystem: true },
  };

  const mockBudgetProgress: BudgetProgress = {
    budget: mockBudget,
    spentAmount: 10000,
    remainingAmount: 40000,
    spentPercentage: 20,
    alertStatus: 'NORMAL'
  };

  describe('createBudget', () => {
    it('should post to correct endpoint', async () => {
      const data: CreateBudgetRequest = { categoryId: 2, budgetYear: 2026, budgetMonth: 5, limitAmount: 50000, alertThreshold: 80 };
      (apiClient.post as any).mockResolvedValue({ data: mockBudget });

      const result = await createBudget(data);

      expect(apiClient.post).toHaveBeenCalledWith('/budgets', data);
      expect(result).toEqual(mockBudget);
    });

    it('should throw error on failure', async () => {
      const data: CreateBudgetRequest = { categoryId: 2, budgetYear: 2026, budgetMonth: 5, limitAmount: 50000, alertThreshold: 80 };
      (apiClient.post as any).mockRejectedValue(new Error('Network Error'));

      await expect(createBudget(data)).rejects.toThrow('Network Error');
    });
  });

  describe('listBudgetsByMonth', () => {
    it('should get from correct endpoint', async () => {
      (apiClient.get as any).mockResolvedValue({ data: [mockBudgetProgress] });

      const result = await listBudgetsByMonth('2026-05');

      expect(apiClient.get).toHaveBeenCalledWith('/budgets/2026-05');
      expect(result).toEqual([mockBudgetProgress]);
    });
  });

  describe('getBudgetProgress', () => {
    it('should get from correct endpoint', async () => {
      (apiClient.get as any).mockResolvedValue({ data: mockBudgetProgress });

      const result = await getBudgetProgress(1);

      expect(apiClient.get).toHaveBeenCalledWith('/budgets/1/progress');
      expect(result).toEqual(mockBudgetProgress);
    });
  });

  describe('deleteBudget', () => {
    it('should call delete on correct endpoint', async () => {
      (apiClient.delete as any).mockResolvedValue({});

      await deleteBudget(1);

      expect(apiClient.delete).toHaveBeenCalledWith('/budgets/1');
    });
  });
});
