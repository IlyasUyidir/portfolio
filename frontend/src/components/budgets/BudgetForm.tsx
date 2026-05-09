import React, { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { X } from 'lucide-react';
import type { Category, Budget } from '../../types';
import { fromCentimes } from '../../utils/formatCurrency';

export interface BudgetFormData {
  categoryId: number;
  monthString: string; // "YYYY-MM"
  limitAmount: number; // For display/input
  alertThreshold: number;
}

interface BudgetFormProps {
  initialData?: Budget | null;
  categories: Category[];
  onSubmit: (data: BudgetFormData) => void;
  onCancel: () => void;
  isLoading?: boolean;
}

export const BudgetForm: React.FC<BudgetFormProps> = ({
  initialData,
  categories,
  onSubmit,
  onCancel,
  isLoading
}) => {
  const defaultMonth = () => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
  };

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors }
  } = useForm<BudgetFormData>({
    defaultValues: {
      categoryId: categories.length > 0 ? categories[0].id : 0,
      monthString: defaultMonth(),
      limitAmount: 0,
      alertThreshold: 80
    }
  });

  useEffect(() => {
    if (initialData) {
      reset({
        categoryId: initialData.category.id,
        monthString: `${initialData.budgetYear}-${String(initialData.budgetMonth).padStart(2, '0')}`,
        limitAmount: Number(fromCentimes(initialData.limitAmount)),
        alertThreshold: initialData.alertThreshold
      });
    } else {
      reset({
        categoryId: categories.length > 0 ? categories[0].id : 0,
        monthString: defaultMonth(),
        limitAmount: 0,
        alertThreshold: 80
      });
    }
  }, [initialData, categories, reset]);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-end bg-black/50 backdrop-blur-sm">
      <div className="bg-bg-base w-full max-w-md h-full shadow-2xl flex flex-col animate-slide-in-right">
        
        <div className="flex items-center justify-between p-6 border-b border-border-subtle">
          <h2 className="text-xl font-bold text-text-primary">
            {initialData ? 'Modifier le budget' : 'Définir un budget'}
          </h2>
          <button onClick={onCancel} className="p-2 text-text-secondary hover:text-text-primary rounded-full hover:bg-bg-input transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-6">
          <form id="budgetForm" onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            
            {/* Category */}
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-2">Catégorie</label>
              <select
                {...register('categoryId', { required: 'La catégorie est requise', valueAsNumber: true })}
                disabled={!!initialData} // Cannot change category of existing budget typically
                className="w-full bg-bg-input border border-border-subtle rounded-xl px-4 py-3 text-text-primary focus:outline-none focus:border-primary disabled:opacity-50"
              >
                {categories.map(cat => (
                  <option key={cat.id} value={cat.id}>{cat.name}</option>
                ))}
              </select>
              {errors.categoryId && <p className="text-danger text-sm mt-1">{errors.categoryId.message}</p>}
            </div>

            {/* Month */}
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-2">Mois</label>
              <input
                type="month"
                {...register('monthString', { required: 'Le mois est requis' })}
                disabled={!!initialData} // Cannot change month of existing budget
                className="w-full bg-bg-input border border-border-subtle rounded-xl px-4 py-3 text-text-primary focus:outline-none focus:border-primary disabled:opacity-50"
              />
              {errors.monthString && <p className="text-danger text-sm mt-1">{errors.monthString.message}</p>}
            </div>

            {/* Limit Amount */}
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-2">Limite (DH)</label>
              <input
                type="number"
                step="0.01"
                min="0.01"
                {...register('limitAmount', { 
                  required: 'La limite est requise',
                  min: { value: 0.01, message: 'La limite doit être supérieure à 0' }
                })}
                className="w-full bg-bg-input border border-border-subtle rounded-xl px-4 py-3 text-text-primary focus:outline-none focus:border-primary placeholder-text-secondary/50"
                placeholder="Ex: 2000.00"
              />
              {errors.limitAmount && <p className="text-danger text-sm mt-1">{errors.limitAmount.message}</p>}
            </div>

            {/* Alert Threshold */}
            <div>
              <label className="block text-sm font-medium text-text-secondary mb-2">Seuil d'alerte (%)</label>
              <div className="flex items-center gap-4">
                <input
                  type="range"
                  min="50"
                  max="100"
                  step="5"
                  {...register('alertThreshold', { valueAsNumber: true })}
                  className="flex-1 accent-primary"
                />
                <input
                  type="number"
                  min="50"
                  max="100"
                  {...register('alertThreshold', { valueAsNumber: true })}
                  className="w-20 bg-bg-input border border-border-subtle rounded-lg px-3 py-2 text-center text-text-primary focus:outline-none focus:border-primary"
                />
              </div>
              <p className="text-xs text-text-secondary mt-2">Vous serez alerté lorsque vos dépenses atteindront ce pourcentage de la limite.</p>
            </div>

          </form>
        </div>

        <div className="p-6 border-t border-border-subtle bg-bg-base">
          <div className="flex gap-4">
            <button
              type="button"
              onClick={onCancel}
              disabled={isLoading}
              className="flex-1 py-3 px-4 rounded-xl font-bold text-text-primary bg-bg-input hover:bg-border-subtle transition-colors disabled:opacity-50"
            >
              Annuler
            </button>
            <button
              type="submit"
              form="budgetForm"
              disabled={isLoading}
              className="flex-1 py-3 px-4 rounded-xl font-bold text-bg-base bg-primary hover:bg-primary-hover transition-colors disabled:opacity-50"
            >
              {isLoading ? 'Enregistrement...' : initialData ? 'Mettre à jour' : 'Créer'}
            </button>
          </div>
        </div>

      </div>
    </div>
  );
};
