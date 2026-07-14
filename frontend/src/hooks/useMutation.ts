import { useState } from 'react';

interface MutationOptions<R> {
  onSuccess?: (data: R) => void;
  onError?: (error: string) => void;
}

/** Shape of an Axios-style error — enough to safely extract the backend message. */
interface ApiError {
  response?: {
    data?: {
      error?: string;
    };
  };
  message?: string;
}

function isApiError(e: unknown): e is ApiError {
  return typeof e === 'object' && e !== null;
}

/**
 * Custom hook for data mutations (POST, PUT, DELETE).
 * Manages loading and error states for async actions.
 */
export function useMutation<T, R>(
  mutationFn: (variables: T) => Promise<R>,
  options?: MutationOptions<R>
) {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const mutate = async (variables: T): Promise<R> => {
    setIsLoading(true);
    setError(null);
    try {
      const result = await mutationFn(variables);
      if (options?.onSuccess) {
        options.onSuccess(result);
      }
      return result;
    } catch (err: unknown) {
      console.error('Mutation error:', err);
      const errorMessage = isApiError(err)
        ? (err.response?.data?.error ?? err.message ?? 'Une erreur est survenue')
        : 'Une erreur est survenue';
      setError(errorMessage);
      if (options?.onError) {
        options.onError(errorMessage);
      }
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const reset = () => {
    setIsLoading(false);
    setError(null);
  };

  return { mutate, isLoading, error, reset };
}
