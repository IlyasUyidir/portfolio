import { useQuery } from '../useQuery';
import { listBudgetsByMonth } from '../../api/budgetApi';
import type { BudgetProgress } from '../../types';

export const useBudgets = (month: string) => {
  return useQuery<BudgetProgress[]>(() => listBudgetsByMonth(month), [month]);
};
