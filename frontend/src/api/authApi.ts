import apiClient from './apiClient';
import type { RegisterRequest, LoginRequest, UserProfile } from '../types';

export const register = async (data: RegisterRequest): Promise<UserProfile> => {
  const response = await apiClient.post<UserProfile>('/auth/register', data);
  return response.data;
};

export const login = async (data: LoginRequest): Promise<UserProfile> => {
  const response = await apiClient.post<UserProfile>('/auth/login', data);
  return response.data;
};

export const logout = async (): Promise<void> => {
  // If the backend has a token invalidation endpoint, it would be called here.
  // Otherwise, it just resolves so the frontend can clear the token.
  try {
    await apiClient.post('/auth/logout');
  } catch (error) {
    console.error('Logout error', error);
  }
};

export const getMe = async (): Promise<UserProfile> => {
  const response = await apiClient.get<UserProfile>('/auth/me');
  return response.data;
};
