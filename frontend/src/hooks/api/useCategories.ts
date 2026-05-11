import { useQuery } from '../useQuery';
import { listCategories } from '../../api/categoryApi';
import type { Category } from '../../types';

export const useCategories = () => {
  return useQuery<Category[]>(() => listCategories(), []);
};
