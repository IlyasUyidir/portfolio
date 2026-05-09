import apiClient from './apiClient';
import type { Category, CreateCategoryRequest } from '../types';

export const listCategories = async (): Promise<Category[]> => {
  const response = await apiClient.get<Category[]>('/categories');
  return response.data;
};

export const createCategory = async (data: CreateCategoryRequest): Promise<Category> => {
  const response = await apiClient.post<Category>('/categories', data);
  return response.data;
};

export const updateCategory = async (id: number, data: CreateCategoryRequest): Promise<Category> => {
  const response = await apiClient.put<Category>(`/categories/${id}`, data);
  return response.data;
};

export const deleteCategory = async (id: number): Promise<void> => {
  await apiClient.delete(`/categories/${id}`);
};
