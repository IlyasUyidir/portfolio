import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate, Link } from 'react-router-dom';
import * as authApi from '../api/authApi';
import type { RegisterRequest } from '../types';
import { useAuth } from '../hooks/useAuth';

export const Register: React.FC = () => {
  const { register, handleSubmit, watch, formState: { errors } } = useForm<RegisterRequest & { confirmPassword?: string }>();
  const navigate = useNavigate();
  const { login } = useAuth();
  const [serverError, setServerError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const password = watch('password');

  const onSubmit = async (data: any) => {
    setIsLoading(true);
    setServerError(null);
    try {
      const payload: RegisterRequest = {
        email: data.email,
        username: data.username,
        password: data.password,
      };
      const user = await authApi.register(payload);
      login(user);
      navigate('/dashboard');
    } catch (error: any) {
      setServerError(error.response?.data?.error || 'Erreur lors de la création du compte');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen w-full bg-bg-base flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-bg-card p-8 rounded-2xl shadow-xl border border-border-subtle">
        <h1 className="text-3xl font-bold text-text-primary text-center mb-8">
          Créer un <span className="text-primary">compte</span>
        </h1>
        
        {serverError && (
          <div className="bg-danger/10 text-danger p-3 rounded-lg mb-6 text-sm text-center">
            {serverError}
          </div>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">Nom d'utilisateur</label>
            <input
              type="text"
              {...register('username', { required: 'Nom requis' })}
              className="w-full bg-bg-input border border-border-subtle rounded-lg px-4 py-2 text-text-primary focus:outline-none focus:border-primary transition-colors"
              placeholder="Ex: Ilyas"
            />
            {errors.username && <p className="text-danger text-sm mt-1">{errors.username.message}</p>}
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">Email</label>
            <input
              type="email"
              {...register('email', { required: 'Email requis' })}
              className="w-full bg-bg-input border border-border-subtle rounded-lg px-4 py-2 text-text-primary focus:outline-none focus:border-primary transition-colors"
              placeholder="votre@email.com"
            />
            {errors.email && <p className="text-danger text-sm mt-1">{errors.email.message}</p>}
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">Mot de passe</label>
            <input
              type="password"
              {...register('password', { 
                required: 'Mot de passe requis',
                minLength: { value: 8, message: 'Minimum 8 caractères' }
              })}
              className="w-full bg-bg-input border border-border-subtle rounded-lg px-4 py-2 text-text-primary focus:outline-none focus:border-primary transition-colors"
              placeholder="••••••••"
            />
            {errors.password && <p className="text-danger text-sm mt-1">{errors.password.message}</p>}
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">Confirmer le mot de passe</label>
            <input
              type="password"
              {...register('confirmPassword', { 
                required: 'Confirmation requise',
                validate: value => value === password || 'Les mots de passe ne correspondent pas'
              })}
              className="w-full bg-bg-input border border-border-subtle rounded-lg px-4 py-2 text-text-primary focus:outline-none focus:border-primary transition-colors"
              placeholder="••••••••"
            />
            {errors.confirmPassword && <p className="text-danger text-sm mt-1">{errors.confirmPassword.message}</p>}
          </div>

          <button
            type="submit"
            disabled={isLoading}
            className="w-full bg-primary hover:bg-primary-hover text-bg-base font-bold py-2 px-4 rounded-lg transition-colors mt-4 disabled:opacity-70"
          >
            {isLoading ? 'Création...' : 'Créer mon compte'}
          </button>
        </form>

        <p className="mt-6 text-center text-text-secondary text-sm">
          Déjà un compte ? <Link to="/login" className="text-primary hover:underline">Se connecter</Link>
        </p>
      </div>
    </div>
  );
};
