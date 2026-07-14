import { renderHook, waitFor } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { useTransactions } from './useTransactions';
import * as transactionApi from '../../api/transactionApi';
import type { TransactionPage } from '../../types';

vi.mock('../../api/transactionApi', () => ({
  listTransactions: vi.fn(),
}));

describe('useTransactions', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // TransactionPage requires `number` (current page) and `size`; Transaction requires `category` and `createdAt`
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
    size: 10,
  };

  it('should return loading initially and then data on success', async () => {
    vi.mocked(transactionApi.listTransactions).mockResolvedValue(mockTransactions);

    const { result } = renderHook(() => useTransactions({ type: 'DEPENSE' }, 0));

    expect(result.current.isLoading).toBe(true);

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.data).toEqual(mockTransactions);
    expect(result.current.error).toBeNull();
    expect(transactionApi.listTransactions).toHaveBeenCalledWith({
      page: 0,
      size: 10,
      startDate: undefined,
      endDate: undefined,
      type: 'DEPENSE',
      categoryId: undefined,
    });
  });

  it('should return error if API call fails', async () => {
    vi.mocked(transactionApi.listTransactions).mockRejectedValue(new Error('Network error'));

    const { result } = renderHook(() => useTransactions({}, 1, 5));

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.error).toBe('Network error');
    expect(result.current.data).toBeNull();
    expect(transactionApi.listTransactions).toHaveBeenCalledWith({
      page: 1,
      size: 5,
      startDate: undefined,
      endDate: undefined,
      type: undefined,
      categoryId: undefined,
    });
  });
});
