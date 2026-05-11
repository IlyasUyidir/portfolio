import { renderHook, waitFor, act } from '@testing-library/react';
import { vi, describe, it, expect } from 'vitest';
import { useQuery } from '../useQuery';

describe('useQuery', () => {
  // 1. Initial loading state
  it('useQuery_shouldStartWithLoadingTrue', () => {
    const fetcher = vi.fn().mockResolvedValue({ name: 'test' });
    const { result } = renderHook(() => useQuery(fetcher, []));
    
    // Arrange & Act (implicit in renderHook)
    
    // Assert
    expect(result.current.isLoading).toBe(true);
    expect(result.current.data).toBeNull();
    expect(result.current.error).toBeNull();
  });

  // 2. Success path
  it('useQuery_onSuccess_shouldSetDataAndSetLoadingFalse', async () => {
    const fetcher = vi.fn().mockResolvedValue({ name: 'test' });
    const { result } = renderHook(() => useQuery(fetcher, []));

    // Act
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    
    // Assert
    expect(result.current.data).toEqual({ name: 'test' });
    expect(result.current.error).toBeNull();
  });

  // 3. Error path (backend error)
  it('useQuery_onError_shouldSetErrorAndSetLoadingFalse', async () => {
    const fetcher = vi.fn().mockRejectedValue({ 
      response: { data: { error: 'Not found' } } 
    });
    const { result } = renderHook(() => useQuery(fetcher, []));

    // Act
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    
    // Assert
    expect(result.current.error).toBe('Not found');
    expect(result.current.data).toBeNull();
  });

  // 4. Error path (generic error)
  it('useQuery_onGenericError_shouldSetFallbackMessage', async () => {
    const fetcher = vi.fn().mockRejectedValue(new Error('Network error'));
    const { result } = renderHook(() => useQuery(fetcher, []));

    // Act
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    
    // Assert
    expect(result.current.error).toBe('Network error');
    expect(result.current.data).toBeNull();
  });

  // 5. Dependency change refetch
  it('useQuery_whenDepsChange_shouldRefetch', async () => {
    const fetcher = vi.fn().mockResolvedValue('data');
    const { result, rerender } = renderHook(
      ({ id }) => useQuery(() => fetcher(id), [id]), 
      { initialProps: { id: 1 } }
    );

    // Arrange: first call
    await waitFor(() => expect(fetcher).toHaveBeenCalledWith(1));
    
    // Act: change dependency
    rerender({ id: 2 });
    
    // Assert: second call
    await waitFor(() => expect(fetcher).toHaveBeenCalledWith(2));
    expect(fetcher).toHaveBeenCalledTimes(2);
  });

  // 6. Manual refetch
  it('useQuery_refetch_shouldCallFetcherAgain', async () => {
    const fetcher = vi.fn().mockResolvedValue('data');
    const { result } = renderHook(() => useQuery(fetcher, []));

    // Arrange: wait for first load
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(fetcher).toHaveBeenCalledTimes(1);

    // Act: manual refetch
    await act(async () => {
      await result.current.refetch();
    });

    // Assert
    expect(fetcher).toHaveBeenCalledTimes(2);
  });

  // 7. Error recovery
  it('useQuery_onErrorThenRefetch_shouldClearError', async () => {
    const fetcher = vi.fn()
      .mockRejectedValueOnce(new Error('First failure'))
      .mockResolvedValueOnce('Success');
      
    const { result } = renderHook(() => useQuery(fetcher, []));

    // Arrange: first call fails
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.error).toBe('First failure');

    // Act: refetch succeeds
    await act(async () => {
      await result.current.refetch();
    });

    // Assert
    expect(result.current.error).toBeNull();
    expect(result.current.data).toBe('Success');
  });
});
