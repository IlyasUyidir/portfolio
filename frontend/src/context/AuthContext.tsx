import React, { createContext, useState, useEffect } from 'react';
import type { ReactNode } from 'react';
import type { UserProfile } from '../types';
import { getToken, setToken, removeToken } from '../utils/tokenStorage';
import * as authApi from '../api/authApi';

interface AuthContextValue {
  user: UserProfile | null;
  isAuthenticated: boolean;
  isPremium: boolean;
  login: (token: string, user: UserProfile) => void;
  logout: () => Promise<void>;
  isLoading: boolean;
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  useEffect(() => {
    const initAuth = async () => {
      const token = getToken();
      if (!token) {
        setIsAuthenticated(false);
        setIsLoading(false);
        return;
      }

      try {
        const userProfile = await authApi.getMe();
        setUser(userProfile);
        setIsAuthenticated(true);
      } catch (error) {
        removeToken();
        setIsAuthenticated(false);
      } finally {
        setIsLoading(false);
      }
    };

    initAuth();
  }, []);

  const login = (token: string, userProfile: UserProfile) => {
    setToken(token);
    setUser(userProfile);
    setIsAuthenticated(true);
  };

  const logout = async () => {
    await authApi.logout();
    removeToken();
    setUser(null);
    setIsAuthenticated(false);
  };

  const isPremium = user?.role === 'PREMIUM' || user?.role === 'ADMIN';

  return (
    <AuthContext.Provider value={{ user, isAuthenticated, isPremium, login, logout, isLoading }}>
      {children}
    </AuthContext.Provider>
  );
};
