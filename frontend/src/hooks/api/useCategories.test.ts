import { renderHook, waitFor } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { useCategories } from './useCategories';
import * as categoryApi from '../../api/categoryApi';
import type { Category } from '../../types';

vi.mock('../../api/categoryApi', () => ({
  listCategories: vi.fn(),
}));

describe('useCategories', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const mockCategory: Category = {
    id: 1, name: 'Food', color: '#f00', type: 'DEPENSE', isSystem: true
  };

  it('should return loading initially and then data on success', async () => {
    (categoryApi.listCategories as any).mockResolvedValue([mockCategory]);

    const { result } = renderHook(() => useCategories());

    expect(result.current.isLoading).toBe(true);
    expect(result.current.data).toBeNull();

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.data).toEqual([mockCategory]);
    expect(result.current.error).toBeNull();
    expect(categoryApi.listCategories).toHaveBeenCalledTimes(1);
  });

  it('should return error if API call fails', async () => {
    (categoryApi.listCategories as any).mockRejectedValue(new Error('Network error'));

    const { result } = renderHook(() => useCategories());

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.error).toBe('Network error');
    expect(result.current.data).toBeNull();
  });
});
