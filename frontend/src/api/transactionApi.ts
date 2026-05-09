import apiClient from './apiClient';
import type { TransactionPage, PaginationParams, Transaction, CreateTransactionRequest } from '../types';

export const listTransactions = async (params: PaginationParams): Promise<TransactionPage> => {
  const response = await apiClient.get<TransactionPage>('/transactions', { params });
  return response.data;
};

export const getTransaction = async (id: number): Promise<Transaction> => {
  const response = await apiClient.get<Transaction>(`/transactions/${id}`);
  return response.data;
};

export const createTransaction = async (data: CreateTransactionRequest): Promise<Transaction> => {
  const response = await apiClient.post<Transaction>('/transactions', data);
  return response.data;
};

export const updateTransaction = async (id: number, data: CreateTransactionRequest): Promise<Transaction> => {
  const response = await apiClient.put<Transaction>(`/transactions/${id}`, data);
  return response.data;
};

export const deleteTransaction = async (id: number): Promise<void> => {
  await apiClient.delete(`/transactions/${id}`);
};
