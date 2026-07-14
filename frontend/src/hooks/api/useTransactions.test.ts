import { renderHook, waitFor } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { useTransactions } from './useTransactions';
import * as transactionApi from '../../api/transactionApi';

vi.mock('../../api/transactionApi', () => ({
  listTransactions: vi.fn(),
}));

describe('useTransactions', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const mockTransactions = {
    content: [{ id: 1, title: 'Lunch', amount: 1500, type: 'DEPENSE', txDate: '2026-05-15', categoryId: 1 }],
    pageable: { pageNumber: 0, pageSize: 10 },
    totalElements: 1,
    totalPages: 1,
    last: true,
  };

  it('should return loading initially and then data on success', async () => {
    (transactionApi.listTransactions as any).mockResolvedValue(mockTransactions);

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
    (transactionApi.listTransactions as any).mockRejectedValue(new Error('Network error'));

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
