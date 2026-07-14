import { renderHook, act } from '@testing-library/react';
import { vi, describe, it, expect } from 'vitest';
import { useMutation } from './useMutation';

describe('useMutation', () => {
  // 1. Initial state
  it('useMutation_shouldStartWithLoadingFalseAndNoError', () => {
    const { result } = renderHook(() => useMutation(vi.fn()));
    
    expect(result.current.isLoading).toBe(false);
    expect(result.current.error).toBeNull();
  });

  // 2. Loading state during mutation
  it('useMutation_whenMutating_shouldSetLoadingTrue', async () => {
    // mutationFn returns a promise that doesn't resolve immediately
    let resolveMutation: (value: any) => void;
    const mutationFn = vi.fn().mockImplementation(() => new Promise((resolve) => {
      resolveMutation = resolve;
    }));

    const { result } = renderHook(() => useMutation(mutationFn));

    // Act
    let mutatePromise: Promise<any>;
    act(() => {
      mutatePromise = result.current.mutate({ data: 'test' });
    });

    // Assert
    expect(result.current.isLoading).toBe(true);
    expect(mutationFn).toHaveBeenCalledWith({ data: 'test' });

    // Cleanup
    await act(async () => {
      resolveMutation!({});
      await mutatePromise!;
    });
  });

  // 3. Success path
  it('useMutation_onSuccess_shouldCallOnSuccessCallback', async () => {
    const onSuccess = vi.fn();
    const mutationFn = vi.fn().mockResolvedValue({ id: 1 });
    const { result } = renderHook(() => useMutation(mutationFn, { onSuccess }));

    // Act
    await act(async () => {
      const data = await result.current.mutate({ name: 'new item' });
      expect(data).toEqual({ id: 1 });
    });

    // Assert
    expect(onSuccess).toHaveBeenCalledWith({ id: 1 });
    expect(result.current.isLoading).toBe(false);
    expect(result.current.error).toBeNull();
  });

  // 4. Error message handling from backend
  it('useMutation_onError_shouldSetErrorMessage', async () => {
    const mutationFn = vi.fn().mockRejectedValue({
      response: { data: { error: 'Validation failed' } }
    });
    const { result } = renderHook(() => useMutation(mutationFn));

    // Act
    await act(async () => {
      try {
        await result.current.mutate({});
      } catch (err) {
        // Expected rejection
      }
    });

    // Assert
    expect(result.current.error).toBe('Validation failed');
    expect(result.current.isLoading).toBe(false);
  });

  // 5. Error callback
  it('useMutation_onError_shouldCallOnErrorCallback', async () => {
    const onError = vi.fn();
    const mutationFn = vi.fn().mockRejectedValue(new Error('Network error'));
    const { result } = renderHook(() => useMutation(mutationFn, { onError }));

    // Act
    await act(async () => {
      try {
        await result.current.mutate({});
      } catch (err) {
        // Expected rejection
      }
    });

    // Assert
    expect(onError).toHaveBeenCalledWith('Network error');
    expect(result.current.error).toBe('Network error');
  });

  // 6. Reset functionality
  it('useMutation_reset_shouldClearError', async () => {
    const mutationFn = vi.fn().mockRejectedValue(new Error('Fail'));
    const { result } = renderHook(() => useMutation(mutationFn));

    // Trigger error
    await act(async () => {
      try {
        await result.current.mutate({});
      } catch (err) {}
    });
    expect(result.current.error).toBe('Fail');

    // Act: reset
    act(() => {
      result.current.reset();
    });

    // Assert
    expect(result.current.error).toBeNull();
    expect(result.current.isLoading).toBe(false);
  });

  // 7. Error propagation to caller
  it('useMutation_shouldPropagateErrorToCallerAfterCallback', async () => {
    const errorObj = { message: 'Critical fail' };
    const mutationFn = vi.fn().mockRejectedValue(errorObj);
    const { result } = renderHook(() => useMutation(mutationFn));

    // Act & Assert
    await act(async () => {
      await expect(result.current.mutate({})).rejects.toEqual(errorObj);
    });
  });
});
