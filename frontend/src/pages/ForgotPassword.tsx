import React from 'react';
import { Link } from 'react-router-dom';

export const ForgotPassword: React.FC = () => {
  return (
    <div className="min-h-screen w-full bg-bg-base flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-bg-card p-8 rounded-2xl shadow-xl border border-border-subtle text-center">
        <h1 className="text-2xl font-bold text-text-primary mb-6">Mot de passe oublié</h1>
        
        <div className="bg-warning/10 text-warning p-6 rounded-xl mb-6">
          <p className="font-semibold text-lg mb-2">Bientôt disponible</p>
          <p className="text-sm">
            La fonctionnalité de réinitialisation de mot de passe est en cours de développement. 
            Veuillez nous contacter si vous ne pouvez pas accéder à votre compte.
          </p>
        </div>

        <Link 
          to="/login" 
          className="w-full inline-block bg-primary hover:bg-primary-hover text-bg-base font-bold py-2 px-4 rounded-lg transition-colors"
        >
          Retour à la connexion
        </Link>
      </div>
    </div>
  );
};
