import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { AppShell } from './layout/AppShell';

export const ProtectedRoute: React.FC = () => {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-screen bg-bg-base text-text-secondary">
        Chargement...
      </div>
    );
  }

  return isAuthenticated ? <AppShell /> : <Navigate to="/login" replace />;
};
