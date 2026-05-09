import apiClient from './apiClient';
import type { Goal, GoalProgress, CreateGoalRequest, ContributeRequest } from '../types';
import { toCentimes, fromCentimes } from '../utils/formatCurrency';

const transformGoal = (goal: any): Goal => ({
  ...goal,
  targetAmount: Number(fromCentimes(goal.targetAmount)),
  currentAmount: Number(fromCentimes(goal.currentAmount)),
});

const transformGoalProgress = (progress: any): GoalProgress => {
  const goal = progress.goal ? transformGoal(progress.goal) : ({} as any);
  return {
    ...progress,
    goalId: goal.id,
    title: goal.title,
    targetAmount: goal.targetAmount,
    currentAmount: goal.currentAmount,
    status: goal.status,
  };
};

export const listGoals = async (): Promise<Goal[]> => {
  const response = await apiClient.get<any[]>('/goals');
  return response.data.map(transformGoal);
};

export const createGoal = async (data: CreateGoalRequest): Promise<Goal> => {
  const payload = {
    ...data,
    targetAmount: toCentimes(data.targetAmount),
  };
  const response = await apiClient.post<any>('/goals', payload);
  return transformGoal(response.data);
};

export const contribute = async (id: number, data: ContributeRequest): Promise<Goal> => {
  const payload = {
    ...data,
    amount: toCentimes(data.amount),
  };
  const response = await apiClient.post<any>(`/goals/${id}/contribute`, payload);
  return transformGoal(response.data);
};

export const getGoalProgress = async (id: number): Promise<GoalProgress> => {
  const response = await apiClient.get<any>(`/goals/${id}/progress`);
  return transformGoalProgress(response.data);
};

export const deleteGoal = async (id: number): Promise<void> => {
  await apiClient.delete(`/goals/${id}`);
};
