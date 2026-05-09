import React, { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { X } from 'lucide-react';
import type { Category, CreateCategoryRequest } from '../../types';

interface CategoryFormProps {
  initialData?: Category | null;
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: CreateCategoryRequest) => Promise<void>;
}

export const CategoryForm: React.FC<CategoryFormProps> = ({
  initialData,
  isOpen,
  onClose,
  onSubmit,
}) => {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<CreateCategoryRequest>({
    defaultValues: {
      name: '',
      type: 'DEPENSE',
      color: '#F5C518',
    },
  });

  useEffect(() => {
    if (isOpen) {
      if (initialData) {
        reset({
          name: initialData.name,
          type: initialData.type,
          color: initialData.color,
        });
      } else {
        reset({
          name: '',
          type: 'DEPENSE',
          color: '#F5C518',
        });
      }
    }
  }, [isOpen, initialData, reset]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-black/60 backdrop-blur-sm">
      <div className="w-full max-w-md bg-[var(--color-bg-base)] h-full overflow-y-auto flex flex-col shadow-2xl border-l border-[var(--color-border-subtle)] animate-in slide-in-from-right">
        
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-[var(--color-border-subtle)] bg-[var(--color-bg-card)]">
          <h2 className="text-xl font-semibold text-[var(--color-text-primary)]">
            {initialData ? 'Modifier la catégorie' : 'Nouvelle catégorie'}
          </h2>
          <button
            onClick={onClose}
            className="p-2 text-[var(--color-text-muted)] hover:text-[var(--color-text-primary)] hover:bg-[var(--color-bg-base)] rounded-full transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Form Body */}
        <div className="p-6 flex-1">
          <form id="category-form" onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            
            {/* Nom */}
            <div className="space-y-1.5">
              <label className="block text-sm font-medium text-[var(--color-text-secondary)]">
                Nom <span className="text-[var(--color-danger)]">*</span>
              </label>
              <input
                {...register('name', { required: 'Le nom est requis' })}
                type="text"
                placeholder="Ex: Voyage"
                className={`w-full px-4 py-2 bg-[var(--color-bg-input)] border rounded-lg text-[var(--color-text-primary)] focus:outline-none focus:border-[var(--color-primary)] transition-colors ${
                  errors.name ? 'border-[var(--color-danger)]' : 'border-[var(--color-border-subtle)]'
                }`}
              />
              {errors.name && <p className="text-xs text-[var(--color-danger)]">{errors.name.message}</p>}
            </div>

            {/* Type */}
            <div className="space-y-1.5">
              <label className="block text-sm font-medium text-[var(--color-text-secondary)]">
                Type <span className="text-[var(--color-danger)]">*</span>
              </label>
              <select
                {...register('type', { required: 'Le type est requis' })}
                className={`w-full px-4 py-2 bg-[var(--color-bg-input)] border rounded-lg text-[var(--color-text-primary)] focus:outline-none focus:border-[var(--color-primary)] transition-colors appearance-none ${
                  errors.type ? 'border-[var(--color-danger)]' : 'border-[var(--color-border-subtle)]'
                }`}
              >
                <option value="DEPENSE">Dépense</option>
                <option value="REVENU">Revenu</option>
                <option value="BOTH">Les deux</option>
              </select>
              {errors.type && <p className="text-xs text-[var(--color-danger)]">{errors.type.message}</p>}
            </div>

            {/* Couleur */}
            <div className="space-y-1.5">
              <label className="block text-sm font-medium text-[var(--color-text-secondary)]">
                Couleur <span className="text-[var(--color-danger)]">*</span>
              </label>
              <div className="flex items-center gap-4">
                <input
                  {...register('color', { required: 'La couleur est requise' })}
                  type="color"
                  className="w-14 h-14 p-1 rounded-lg bg-[var(--color-bg-input)] border border-[var(--color-border-subtle)] cursor-pointer"
                />
                <span className="text-sm text-[var(--color-text-secondary)]">
                  Choisissez une couleur pour identifier facilement cette catégorie.
                </span>
              </div>
              {errors.color && <p className="text-xs text-[var(--color-danger)]">{errors.color.message}</p>}
            </div>

          </form>
        </div>

        {/* Footer actions */}
        <div className="p-6 border-t border-[var(--color-border-subtle)] bg-[var(--color-bg-card)] flex justify-end gap-3">
          <button
            type="button"
            onClick={onClose}
            disabled={isSubmitting}
            className="px-4 py-2 rounded-lg text-[var(--color-text-secondary)] hover:bg-[var(--color-border-subtle)] hover:text-[var(--color-text-primary)] transition-colors disabled:opacity-50"
          >
            Annuler
          </button>
          <button
            type="submit"
            form="category-form"
            disabled={isSubmitting}
            className="px-4 py-2 rounded-lg bg-[var(--color-primary)] text-[var(--color-bg-base)] font-medium hover:bg-[var(--color-primary-hover)] transition-colors disabled:opacity-50 flex items-center"
          >
            {isSubmitting ? 'Enregistrement...' : 'Enregistrer la catégorie'}
          </button>
        </div>

      </div>
    </div>
  );
};
