import { useQuery } from '../useQuery';
import { listTransactions } from '../../api/transactionApi';
import type { FilterState } from '../../components/transactions/TransactionFilters';

export const useTransactions = (filters: FilterState, page: number, size: number = 10) => {
  return useQuery(async () => {
    return await listTransactions({
      page,
      size,
      startDate: filters.startDate || undefined,
      endDate: filters.endDate || undefined,
      type: filters.type || undefined,
      categoryId: filters.categoryId || undefined,
    });
  }, [page, size, JSON.stringify(filters)]); // JSON.stringify to catch deep filter changes
};
