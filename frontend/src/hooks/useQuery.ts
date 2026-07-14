import { useState, useEffect, useCallback, type DependencyList } from 'react';

interface QueryState<T> {
  data: T | null;
  isLoading: boolean;
  error: string | null;
  refetch: () => Promise<void>;
}

/**
 * Custom hook for data fetching.
 * Extracts loading, error and data management from components.
 */
export function useQuery<T>(
  fetcher: () => Promise<T>,
  deps: DependencyList = []
): QueryState<T> {
  const [data, setData] = useState<T | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const execute = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const result = await fetcher();
      setData(result);
    } catch (err: unknown) {
      console.error('Query error:', err);
      const e = err as { response?: { data?: { error?: string } }, message?: string };
      const errorMessage = e.response?.data?.error ?? e.message ?? 'Une erreur est survenue';
      setError(errorMessage);
    } finally {
      setIsLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps, react-hooks/use-memo
  }, deps);

  useEffect(() => {
    execute();
  }, [execute]);

  return { data, isLoading, error, refetch: execute };
}
