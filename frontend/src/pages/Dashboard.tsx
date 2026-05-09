import React from 'react';
import { useAuth } from '../hooks/useAuth';

export const Dashboard: React.FC = () => {
  const { user, logout } = useAuth();

  return (
    <div className="p-8">
      <h1 className="text-3xl font-bold text-text-primary mb-4">Tableau de bord</h1>
      <p className="text-text-secondary mb-8">Bienvenue, {user?.username} !</p>
      
      <button 
        onClick={logout}
        className="bg-danger hover:bg-danger/80 text-white font-bold py-2 px-4 rounded-lg transition-colors"
      >
        Se déconnecter
      </button>
    </div>
  );
};
