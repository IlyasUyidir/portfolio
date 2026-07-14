import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import * as authApi from '../api/authApi';
import type { LoginRequest } from '../types';

export const Login: React.FC = () => {
  const { register, handleSubmit, formState: { errors } } = useForm<LoginRequest>();
  const { login } = useAuth();
  const navigate = useNavigate();
  const [serverError, setServerError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const onSubmit = async (data: LoginRequest) => {
    setIsLoading(true);
    setServerError(null);
    try {
      const user = await authApi.login(data);
      login(user);
      navigate('/dashboard');
    } catch (error: unknown) {
      setServerError(error.response?.data?.error || 'Identifiants incorrects');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex h-screen w-full bg-bg-base">
      {/* Left Panel */}
      <div className="hidden lg:flex flex-col justify-center items-center w-1/2 bg-bg-sidebar p-12 text-center border-r border-border-subtle">
        <h1 className="text-4xl font-bold text-text-primary mb-4">Folio.io</h1>
        <p className="text-text-secondary text-lg max-w-md">Gérez vos finances personnelles en toute simplicité et atteignez vos objectifs.</p>
      </div>

      {/* Right Panel - Form */}
      <div className="flex flex-col justify-center items-center w-full lg:w-1/2 p-8">
        <div className="w-full max-w-md bg-bg-card p-8 rounded-2xl shadow-xl border border-border-subtle">
          <div className="lg:hidden mb-8 text-center">
            <h1 className="text-3xl font-bold text-text-primary">Portefeuille <span className="text-primary">Intelligent</span></h1>
          </div>

          <h2 className="text-2xl font-bold text-text-primary mb-6 text-center">Se connecter</h2>

          {serverError && (
            <div className="bg-danger/10 text-danger p-3 rounded-lg mb-6 text-sm text-center">
              {serverError}
            </div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
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
              <div className="flex justify-between items-center mb-1">
                <label className="block text-sm font-medium text-text-secondary">Mot de passe</label>
                <Link to="/forgot-password" className="text-xs text-primary hover:text-primary-hover">Mot de passe oublié ?</Link>
              </div>
              <input
                type="password"
                {...register('password', { required: 'Mot de passe requis' })}
                className="w-full bg-bg-input border border-border-subtle rounded-lg px-4 py-2 text-text-primary focus:outline-none focus:border-primary transition-colors"
                placeholder="••••••••"
              />
              {errors.password && <p className="text-danger text-sm mt-1">{errors.password.message}</p>}
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full bg-primary hover:bg-primary-hover text-bg-base font-bold py-2 px-4 rounded-lg transition-colors mt-2 disabled:opacity-70"
            >
              {isLoading ? 'Connexion...' : 'Se connecter'}
            </button>
          </form>

          <div className="mt-6 flex items-center justify-between">
            <hr className="w-full border-border-subtle" />
            <span className="p-2 text-text-muted text-sm bg-bg-card">OU</span>
            <hr className="w-full border-border-subtle" />
          </div>

          <button
            type="button"
            className="w-full mt-6 bg-transparent border border-border-subtle text-text-primary hover:bg-bg-input py-2 px-4 rounded-lg transition-colors flex items-center justify-center gap-2"
          >
            <svg className="w-5 h-5" viewBox="0 0 24 24">
              <path fill="currentColor" d="M12.545,10.239v3.821h5.445c-0.712,2.315-2.647,3.972-5.445,3.972c-3.332,0-6.033-2.701-6.033-6.032s2.701-6.032,6.033-6.032c1.498,0,2.866,0.549,3.921,1.453l2.814-2.814C17.503,2.988,15.139,2,12.545,2C7.021,2,2.543,6.477,2.543,12s4.478,10,10.002,10c8.396,0,10.249-7.85,9.426-11.748L12.545,10.239z" />
            </svg>
            Continuer avec Google
          </button>

          <p className="mt-6 text-center text-text-secondary text-sm">
            Pas encore de compte ? <Link to="/register" className="text-primary hover:underline">Créer un compte</Link>
          </p>
        </div>
      </div>
    </div>
  );
};
