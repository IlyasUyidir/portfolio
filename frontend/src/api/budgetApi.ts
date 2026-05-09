import apiClient from './apiClient';
import type { Budget, BudgetProgress, CreateBudgetRequest } from '../types';

export const createBudget = async (data: CreateBudgetRequest): Promise<Budget> => {
  const response = await apiClient.post<Budget>('/budgets', data);
  return response.data;
};

export const listBudgetsByMonth = async (month: string): Promise<BudgetProgress[]> => {
  // month is "YYYY-MM"
  const response = await apiClient.get<BudgetProgress[]>(`/budgets/${month}`);
  return response.data;
};

export const getBudgetProgress = async (id: number): Promise<BudgetProgress> => {
  const response = await apiClient.get<BudgetProgress>(`/budgets/${id}/progress`);
  return response.data;
};

export const deleteBudget = async (id: number): Promise<void> => {
  await apiClient.delete(`/budgets/${id}`);
};
