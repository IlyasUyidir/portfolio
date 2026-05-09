import React from 'react';
import { useForm } from 'react-hook-form';

interface GoalFormValues {
  title: string;
  targetAmount: number;
  targetDate: string;
}

interface GoalFormProps {
  onSuccess: (data: { title: string; targetAmount: number; targetDate: string }) => void;
  onCancel: () => void;
}

export const GoalForm: React.FC<GoalFormProps> = ({ onSuccess, onCancel }) => {
  const { register, handleSubmit, formState: { errors } } = useForm<GoalFormValues>();

  const onSubmit = (data: GoalFormValues) => {
    onSuccess(data);
  };

  return (
    <div className="bg-bg-card p-6 rounded-xl border border-border-subtle">
      <h3 className="text-xl font-bold text-text-primary mb-6">Nouvel Objectif</h3>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-text-primary mb-1">
            Titre de l'objectif *
          </label>
          <input
            type="text"
            className="w-full bg-bg-input border border-border-subtle text-text-primary rounded-lg px-4 py-2 focus:outline-none focus:border-primary"
            placeholder="Ex: Voyage au Japon"
            {...register('title', { required: 'Le titre est requis' })}
          />
          {errors.title && <p className="text-danger text-sm mt-1">{errors.title.message}</p>}
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-text-primary mb-1">
              Montant cible (DH) *
            </label>
            <input
              type="number"
              step="0.01"
              className="w-full bg-bg-input border border-border-subtle text-text-primary rounded-lg px-4 py-2 focus:outline-none focus:border-primary"
              placeholder="Ex: 20000"
              {...register('targetAmount', { 
                required: 'Le montant cible est requis',
                min: { value: 1, message: 'Le montant doit être supérieur à 0' }
              })}
            />
            {errors.targetAmount && <p className="text-danger text-sm mt-1">{errors.targetAmount.message}</p>}
          </div>

          <div>
            <label className="block text-sm font-medium text-text-primary mb-1">
              Date cible *
            </label>
            <input
              type="date"
              className="w-full bg-bg-input border border-border-subtle text-text-primary rounded-lg px-4 py-2 focus:outline-none focus:border-primary color-scheme-dark"
              style={{ colorScheme: 'dark' }}
              {...register('targetDate', { required: 'La date cible est requise' })}
            />
            {errors.targetDate && <p className="text-danger text-sm mt-1">{errors.targetDate.message}</p>}
          </div>
        </div>

        <div className="flex justify-end gap-3 pt-4 border-t border-border-subtle">
          <button
            type="button"
            onClick={onCancel}
            className="px-4 py-2 rounded-lg text-text-secondary hover:bg-border-subtle hover:text-text-primary transition-colors"
          >
            Annuler
          </button>
          <button
            type="submit"
            className="px-4 py-2 rounded-lg bg-primary text-bg-base font-semibold hover:bg-primary-hover transition-colors"
          >
            Créer l'objectif
          </button>
        </div>
      </form>
    </div>
  );
};
