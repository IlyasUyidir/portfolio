import { describe, it, expect, vi, beforeEach } from 'vitest';
import apiClient from './apiClient';
import { register, login, logout, getMe } from './authApi';
import type { UserProfile, RegisterRequest, LoginRequest } from '../types';

// Mock the apiClient module
vi.mock('./apiClient', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
  }
}));

describe('authApi', () => {
  const mockUser: UserProfile = {
    id: 1,
    email: 'test@folio.io',
    username: 'testuser',
    role: 'STANDARD',
    createdAt: '2026-05-11T18:00:00Z',
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('register', () => {
    it('register_shouldPostToCorrectEndpoint', async () => {
      // Arrange
      const registerData: RegisterRequest = {
        email: 'test@folio.io',
        username: 'testuser',
        password: 'password123',
      };
      (apiClient.post as any).mockResolvedValue({ data: mockUser });

      // Act
      await register(registerData);

      // Assert
      expect(apiClient.post).toHaveBeenCalledWith('/auth/register', registerData);
    });

    it('register_shouldReturnUserProfile', async () => {
      // Arrange
      const registerData: RegisterRequest = {
        email: 'test@folio.io',
        username: 'testuser',
        password: 'password123',
      };
      (apiClient.post as any).mockResolvedValue({ data: mockUser });

      // Act
      const result = await register(registerData);

      // Assert
      expect(result).toEqual(mockUser);
      expect(result.email).toBe('test@folio.io');
    });
  });

  describe('login', () => {
    it('login_shouldPostToCorrectEndpoint', async () => {
      // Arrange
      const loginData: LoginRequest = {
        email: 'test@folio.io',
        password: 'password123',
      };
      (apiClient.post as any).mockResolvedValue({ data: mockUser });

      // Act
      await login(loginData);

      // Assert
      expect(apiClient.post).toHaveBeenCalledWith('/auth/login', loginData);
    });

    it('login_shouldReturnUserProfile', async () => {
      // Arrange
      const loginData: LoginRequest = {
        email: 'test@folio.io',
        password: 'password123',
      };
      (apiClient.post as any).mockResolvedValue({ data: mockUser });

      // Act
      const result = await login(loginData);

      // Assert
      expect(result).toEqual(mockUser);
      expect(result).toHaveProperty('role', 'STANDARD');
    });
  });

  describe('logout', () => {
    it('logout_shouldPostToLogoutEndpoint', async () => {
      // Arrange
      (apiClient.post as any).mockResolvedValue({});

      // Act
      await logout();

      // Assert
      expect(apiClient.post).toHaveBeenCalledWith('/auth/logout');
    });

    it('logout_whenApiThrows_shouldNotPropagateError', async () => {
      // Arrange
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
      (apiClient.post as any).mockRejectedValue(new Error('Network error'));

      // Act & Assert
      await expect(logout()).resolves.toBeUndefined();
      expect(consoleSpy).toHaveBeenCalled();
      
      consoleSpy.mockRestore();
    });
  });

  describe('getMe', () => {
    it('getMe_shouldGetFromCorrectEndpoint', async () => {
      // Arrange
      (apiClient.get as any).mockResolvedValue({ data: mockUser });

      // Act
      const result = await getMe();

      // Assert
      expect(apiClient.get).toHaveBeenCalledWith('/auth/me');
      expect(result).toEqual(mockUser);
    });
  });
});
