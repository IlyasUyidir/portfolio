import apiClient from './apiClient';
import type { DashboardKpis, SpendingCategory } from '../types';

export const getKpis = async (month: string): Promise<DashboardKpis> => {
  const response = await apiClient.get<DashboardKpis>('/dashboard/kpis', {
    params: { month },
  });
  return response.data;
};

export const getSpending = async (): Promise<SpendingCategory[]> => {
  const response = await apiClient.get<SpendingCategory[]>('/dashboard/spending');
  return response.data;
};
