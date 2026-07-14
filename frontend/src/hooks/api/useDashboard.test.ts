import { renderHook, waitFor } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { useDashboard } from './useDashboard';
import * as dashboardApi from '../../api/dashboardApi';
import * as transactionApi from '../../api/transactionApi';
import * as budgetApi from '../../api/budgetApi';

vi.mock('../../api/dashboardApi', () => ({
  getKpis: vi.fn(),
  getSpending: vi.fn(),
}));

vi.mock('../../api/transactionApi', () => ({
  listTransactions: vi.fn(),
}));

vi.mock('../../api/budgetApi', () => ({
  listBudgetsByMonth: vi.fn(),
}));

describe('useDashboard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const mockKpis = { totalIncome: 1000, totalExpenses: 500, monthlyBalance: 500, savingsRate: 50 };
  const mockSpending = [{ categoryId: 1, categoryName: 'Food', categoryColor: '#f00', totalAmount: 500, percentage: 100 }];
  const mockTransactions = {
    content: [{ id: 1, title: 'Lunch', amount: 1500, type: 'DEPENSE', txDate: '2026-05-15', categoryId: 1 }],
    pageable: { pageNumber: 0, pageSize: 5 },
    totalElements: 1,
    totalPages: 1,
    last: true,
  };
  const mockBudgets = [
    { budget: { id: 1 }, alertStatus: 'CRITICAL' },
    { budget: { id: 2 }, alertStatus: 'OK' },
    { budget: { id: 3 }, alertStatus: 'WARNING' },
  ];

  it('should return loading initially and then combined data on success', async () => {
    (dashboardApi.getKpis as any).mockResolvedValue(mockKpis);
    (dashboardApi.getSpending as any).mockResolvedValue(mockSpending);
    (transactionApi.listTransactions as any).mockResolvedValue(mockTransactions);
    (budgetApi.listBudgetsByMonth as any).mockResolvedValue(mockBudgets);

    const { result } = renderHook(() => useDashboard('2026-05'));

    expect(result.current.isLoading).toBe(true);

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.data?.kpis).toEqual(mockKpis);
    expect(result.current.data?.spending).toEqual(mockSpending);
    expect(result.current.data?.recentTransactions).toEqual(mockTransactions.content);
    
    // Check sorting of active alerts
    expect(result.current.data?.budgetAlerts).toHaveLength(2);
    expect(result.current.data?.budgetAlerts[0].alertStatus).toBe('CRITICAL');
    expect(result.current.data?.budgetAlerts[1].alertStatus).toBe('WARNING');
    expect(result.current.error).toBeNull();
  });

  it('should return error if any API call fails', async () => {
    (dashboardApi.getKpis as any).mockRejectedValue(new Error('Kpi error'));
    (dashboardApi.getSpending as any).mockResolvedValue(mockSpending);
    (transactionApi.listTransactions as any).mockResolvedValue(mockTransactions);
    (budgetApi.listBudgetsByMonth as any).mockResolvedValue(mockBudgets);

    const { result } = renderHook(() => useDashboard('2026-05'));

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.error).toBe('Kpi error');
    expect(result.current.data).toBeNull();
  });
});
