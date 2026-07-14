import { describe, it, expect, vi, beforeEach } from 'vitest';
import apiClient from './apiClient';
import { getKpis, getSpending } from './dashboardApi';
import type { DashboardKpis, SpendingCategory } from '../types';

vi.mock('./apiClient', () => ({
  default: {
    get: vi.fn(),
  }
}));

describe('dashboardApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const mockKpis: DashboardKpis = {
    totalIncome: 1000,
    totalExpenses: 500,
    monthlyBalance: 500,
    savingsRate: 50,
  };

  const mockSpending: SpendingCategory = {
    category: { id: 1, name: 'Food', color: '#ff0000', type: 'DEPENSE', isSystem: true },
    totalAmount: 500,
  };

  describe('getKpis', () => {
    it('should get from correct endpoint with params', async () => {
      (apiClient.get as any).mockResolvedValue({ data: mockKpis });

      const result = await getKpis('2026-05');

      expect(apiClient.get).toHaveBeenCalledWith('/dashboard/kpis', { params: { month: '2026-05' } });
      expect(result).toEqual(mockKpis);
    });

    it('should throw error on failure', async () => {
      (apiClient.get as any).mockRejectedValue(new Error('Network Error'));

      await expect(getKpis('2026-05')).rejects.toThrow('Network Error');
    });
  });

  describe('getSpending', () => {
    it('should get from correct endpoint', async () => {
      (apiClient.get as any).mockResolvedValue({ data: [mockSpending] });

      const result = await getSpending();

      expect(apiClient.get).toHaveBeenCalledWith('/dashboard/spending');
      expect(result).toEqual([mockSpending]);
    });
  });
});
