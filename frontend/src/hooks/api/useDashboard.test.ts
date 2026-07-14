import { renderHook, waitFor } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { useDashboard } from './useDashboard';
import * as dashboardApi from '../../api/dashboardApi';
import * as transactionApi from '../../api/transactionApi';
import * as budgetApi from '../../api/budgetApi';
import type { SpendingCategory, TransactionPage, BudgetProgress } from '../../types';

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

  // SpendingCategory requires a nested Category object
  const mockSpending: SpendingCategory[] = [
    {
      category: { id: 1, name: 'Food', color: '#f00', type: 'DEPENSE', isSystem: false },
      totalAmount: 500,
    },
  ];

  // TransactionPage requires `number` (current page) and `size` fields
  const mockTransactions: TransactionPage = {
    content: [
      {
        id: 1,
        title: 'Lunch',
        amount: 1500,
        type: 'DEPENSE',
        category: { id: 1, name: 'Food', color: '#f00', type: 'DEPENSE', isSystem: false },
        txDate: '2026-05-15',
        createdAt: '2026-05-15T12:00:00Z',
      },
    ],
    totalElements: 1,
    totalPages: 1,
    number: 0,
    size: 5,
  };

  // BudgetProgress requires spentAmount, remainingAmount, spentPercentage, and a full Budget
  const mockBudgets: BudgetProgress[] = [
    {
      budget: {
        id: 1,
        category: { id: 1, name: 'Food', color: '#f00', type: 'DEPENSE', isSystem: false },
        budgetYear: 2026,
        budgetMonth: 5,
        limitAmount: 50000,
        alertThreshold: 80,
      },
      spentAmount: 50000,
      remainingAmount: 0,
      spentPercentage: 100,
      alertStatus: 'CRITICAL',
    },
    {
      budget: {
        id: 2,
        category: { id: 2, name: 'Transport', color: '#00f', type: 'DEPENSE', isSystem: false },
        budgetYear: 2026,
        budgetMonth: 5,
        limitAmount: 20000,
        alertThreshold: 80,
      },
      spentAmount: 5000,
      remainingAmount: 15000,
      spentPercentage: 25,
      alertStatus: 'NORMAL',
    },
    {
      budget: {
        id: 3,
        category: { id: 3, name: 'Leisure', color: '#0f0', type: 'DEPENSE', isSystem: false },
        budgetYear: 2026,
        budgetMonth: 5,
        limitAmount: 30000,
        alertThreshold: 80,
      },
      spentAmount: 25000,
      remainingAmount: 5000,
      spentPercentage: 83,
      alertStatus: 'WARNING',
    },
  ];

  it('should return loading initially and then combined data on success', async () => {
    vi.mocked(dashboardApi.getKpis).mockResolvedValue(mockKpis);
    vi.mocked(dashboardApi.getSpending).mockResolvedValue(mockSpending);
    vi.mocked(transactionApi.listTransactions).mockResolvedValue(mockTransactions);
    vi.mocked(budgetApi.listBudgetsByMonth).mockResolvedValue(mockBudgets);

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
    vi.mocked(dashboardApi.getKpis).mockRejectedValue(new Error('Kpi error'));
    vi.mocked(dashboardApi.getSpending).mockResolvedValue(mockSpending);
    vi.mocked(transactionApi.listTransactions).mockResolvedValue(mockTransactions);
    vi.mocked(budgetApi.listBudgetsByMonth).mockResolvedValue(mockBudgets);

    const { result } = renderHook(() => useDashboard('2026-05'));

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.error).toBe('Kpi error');
    expect(result.current.data).toBeNull();
  });
});
