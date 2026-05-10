import apiClient from './apiClient';
import type { Goal, GoalProgress, CreateGoalRequest, ContributeRequest, GoalStatus } from '../types';
import { toCentimes, fromCentimes } from '../utils/formatCurrency';

interface GoalApiResponse {
  id: number;
  userId?: number;
  title: string;
  targetAmount: number;
  currentAmount: number;
  targetDate: string;
  status: string;
  createdAt: string;
}

interface GoalProgressApiResponse {
  goalId: number;
  title: string;
  targetAmount: number;
  currentAmount: number;
  progressPercentage: number;
  milestones: {
    twentyFive: boolean;
    fifty: boolean;
    seventyFive: boolean;
    hundred: boolean;
  };
  status: string;
  goal?: GoalApiResponse;
}

const transformGoal = (goal: GoalApiResponse): Goal => ({
  ...goal,
  targetAmount: Number(fromCentimes(goal.targetAmount)),
  currentAmount: Number(fromCentimes(goal.currentAmount)),
  status: goal.status as GoalStatus,
});

const transformGoalProgress = (progress: GoalProgressApiResponse): GoalProgress => {
  const goal = progress.goal ? transformGoal(progress.goal) : ({} as Goal);
  return {
    ...progress,
    goalId: progress.goalId || goal.id,
    title: progress.title || goal.title,
    targetAmount: progress.targetAmount || goal.targetAmount,
    currentAmount: progress.currentAmount || goal.currentAmount,
    status: (progress.status || goal.status) as GoalStatus,
  };
};

export const listGoals = async (): Promise<Goal[]> => {
  const response = await apiClient.get<GoalApiResponse[]>('/goals');
  return response.data.map(transformGoal);
};

export const createGoal = async (data: CreateGoalRequest): Promise<Goal> => {
  const payload = {
    ...data,
    targetAmount: toCentimes(data.targetAmount),
  };
  const response = await apiClient.post<GoalApiResponse>('/goals', payload);
  return transformGoal(response.data);
};

export const contribute = async (id: number, data: ContributeRequest): Promise<Goal> => {
  const payload = {
    ...data,
    amount: toCentimes(data.amount),
  };
  const response = await apiClient.post<GoalApiResponse>(`/goals/${id}/contribute`, payload);
  return transformGoal(response.data);
};

export const getGoalProgress = async (id: number): Promise<GoalProgress> => {
  const response = await apiClient.get<GoalProgressApiResponse>(`/goals/${id}/progress`);
  return transformGoalProgress(response.data);
};

export const deleteGoal = async (id: number): Promise<void> => {
  await apiClient.delete(`/goals/${id}`);
};