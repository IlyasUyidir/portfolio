import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link } from 'react-router-dom';

export const ForgotPassword: React.FC = () => {
  const { register, handleSubmit, formState: { errors } } = useForm<{ email: string }>();
  const [isSent, setIsSent] = useState(false);

  const onSubmit = () => {
    // Fake API call as per spec
    setIsSent(true);
  };

  return (
    <div className="min-h-screen w-full bg-bg-base flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-bg-card p-8 rounded-2xl shadow-xl border border-border-subtle">
        <h1 className="text-2xl font-bold text-text-primary text-center mb-6">Mot de passe oublié</h1>
        
        {isSent ? (
          <div className="text-center">
            <div className="bg-success/10 text-success p-4 rounded-lg mb-6 text-sm">
              Si un compte existe pour cette adresse, vous recevrez un email contenant un lien de réinitialisation.
            </div>
            <Link to="/login" className="text-primary hover:underline block mt-4 text-center w-full">
              Retour à la connexion
            </Link>
          </div>
        ) : (
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <p className="text-text-secondary text-sm text-center mb-6">
              Entrez votre adresse email et nous vous enverrons un lien pour réinitialiser votre mot de passe.
            </p>

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

            <button
              type="submit"
              className="w-full bg-primary hover:bg-primary-hover text-bg-base font-bold py-2 px-4 rounded-lg transition-colors mt-4"
            >
              Envoyer le lien
            </button>
            
            <Link to="/login" className="text-text-secondary hover:text-text-primary text-sm block mt-4 text-center w-full transition-colors">
              Annuler
            </Link>
          </form>
        )}
      </div>
    </div>
  );
};
