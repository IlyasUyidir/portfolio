import React, { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { X } from 'lucide-react';
import type { Category, TransactionType, Transaction } from '../../types';
import { fromCentimes } from '../../utils/formatCurrency';

export interface TransactionFormData {
  title: string;
  amount: string; // handled as string in form, converted on submit
  type: TransactionType;
  categoryId: number;
  txDate: string;
  description?: string;
}

interface TransactionFormProps {
  mode: 'create' | 'edit';
  initialData?: Transaction | null;
  categories: Category[];
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: TransactionFormData) => Promise<void>;
}

export const TransactionForm: React.FC<TransactionFormProps> = ({
  mode,
  initialData,
  categories,
  isOpen,
  onClose,
  onSubmit,
}) => {
  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<TransactionFormData>({
    defaultValues: {
      title: '',
      amount: '',
      type: 'DEPENSE',
      categoryId: undefined,
      txDate: new Date().toISOString().split('T')[0],
      description: '',
    },
  });

  const selectedType = watch('type');

  // Filter categories based on selected type (or BOTH)
  const filteredCategories = categories.filter(
    (c) => c.type === selectedType || c.type === 'BOTH'
  );

  useEffect(() => {
    if (isOpen) {
      if (mode === 'edit' && initialData) {
        reset({
          title: initialData.title,
          amount: fromCentimes(initialData.amount),
          type: initialData.type,
          categoryId: initialData.category.id,
          txDate: initialData.txDate,
          description: initialData.description || '',
        });
      } else {
        reset({
          title: '',
          amount: '',
          type: 'DEPENSE',
          categoryId: undefined,
          txDate: new Date().toISOString().split('T')[0],
          description: '',
        });
      }
    }
  }, [isOpen, mode, initialData, reset]);

  const handleFormSubmit = async (data: TransactionFormData) => {
    // We pass data back to parent, but parent will need to handle toCentimes logic 
    // or we can do it here. Let's do it in the parent or prepare it correctly.
    // The prompt says "On submit: toCentimes(amount) before sending to API"
    // So the parent component `Transactions.tsx` will receive the form data with the raw string amount,
    // and will call `toCentimes` before sending to the API.
    await onSubmit(data);
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-black/60 backdrop-blur-sm">
      <div className="w-full max-w-md bg-[var(--color-bg-base)] h-full overflow-y-auto flex flex-col shadow-2xl border-l border-[var(--color-border-subtle)] animate-in slide-in-from-right">
        
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-[var(--color-border-subtle)] bg-[var(--color-bg-card)]">
          <h2 className="text-xl font-semibold text-[var(--color-text-primary)]">
            {mode === 'create' ? 'Nouvelle transaction' : 'Modifier la transaction'}
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
          <form id="transaction-form" onSubmit={handleSubmit(handleFormSubmit)} className="space-y-6">
            
            {/* Type Toggle */}
            <div className="flex p-1 bg-[var(--color-bg-input)] rounded-lg">
              <button
                type="button"
                onClick={() => {
                  setValue('type', 'DEPENSE');
                  setValue('categoryId', 0); // reset category on type change
                }}
                className={`flex-1 py-2 text-sm font-medium rounded-md transition-all ${
                  selectedType === 'DEPENSE'
                    ? 'bg-[var(--color-bg-card)] text-[var(--color-danger)] shadow-sm'
                    : 'text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]'
                }`}
              >
                Dépense
              </button>
              <button
                type="button"
                onClick={() => {
                  setValue('type', 'REVENU');
                  setValue('categoryId', 0); // reset category on type change
                }}
                className={`flex-1 py-2 text-sm font-medium rounded-md transition-all ${
                  selectedType === 'REVENU'
                    ? 'bg-[var(--color-bg-card)] text-[var(--color-success)] shadow-sm'
                    : 'text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]'
                }`}
              >
                Revenu
              </button>
            </div>

            {/* Titre */}
            <div className="space-y-1.5">
              <label className="block text-sm font-medium text-[var(--color-text-secondary)]">
                Titre <span className="text-[var(--color-danger)]">*</span>
              </label>
              <input
                {...register('title', { required: 'Le titre est requis' })}
                type="text"
                placeholder="Ex: Courses Carrefour"
                className={`w-full px-4 py-2 bg-[var(--color-bg-input)] border rounded-lg text-[var(--color-text-primary)] focus:outline-none focus:border-[var(--color-primary)] transition-colors ${
                  errors.title ? 'border-[var(--color-danger)]' : 'border-[var(--color-border-subtle)]'
                }`}
              />
              {errors.title && <p className="text-xs text-[var(--color-danger)]">{errors.title.message}</p>}
            </div>

            {/* Montant */}
            <div className="space-y-1.5">
              <label className="block text-sm font-medium text-[var(--color-text-secondary)]">
                Montant <span className="text-[var(--color-danger)]">*</span>
              </label>
              <div className="relative">
                <input
                  {...register('amount', {
                    required: 'Le montant est requis',
                    min: { value: 0.01, message: 'Le montant doit être supérieur à 0' },
                    pattern: {
                      value: /^\d+(\.\d{1,2})?$/,
                      message: 'Format invalide (ex: 320.50)'
                    }
                  })}
                  type="text"
                  placeholder="0.00"
                  className={`w-full px-4 py-2 bg-[var(--color-bg-input)] border rounded-lg text-[var(--color-text-primary)] focus:outline-none focus:border-[var(--color-primary)] transition-colors pr-12 ${
                    errors.amount ? 'border-[var(--color-danger)]' : 'border-[var(--color-border-subtle)]'
                  }`}
                />
                <div className="absolute inset-y-0 right-0 flex items-center pr-4 pointer-events-none text-[var(--color-text-muted)]">
                  DH
                </div>
              </div>
              {errors.amount && <p className="text-xs text-[var(--color-danger)]">{errors.amount.message}</p>}
            </div>

            {/* Catégorie */}
            <div className="space-y-1.5">
              <label className="block text-sm font-medium text-[var(--color-text-secondary)]">
                Catégorie <span className="text-[var(--color-danger)]">*</span>
              </label>
              <select
                {...register('categoryId', { 
                  required: 'La catégorie est requise',
                  valueAsNumber: true,
                  validate: (val) => val > 0 || 'Veuillez sélectionner une catégorie'
                })}
                className={`w-full px-4 py-2 bg-[var(--color-bg-input)] border rounded-lg text-[var(--color-text-primary)] focus:outline-none focus:border-[var(--color-primary)] transition-colors appearance-none ${
                  errors.categoryId ? 'border-[var(--color-danger)]' : 'border-[var(--color-border-subtle)]'
                }`}
              >
                <option value={0} disabled>Sélectionner une catégorie...</option>
                {filteredCategories.map((cat) => (
                  <option key={cat.id} value={cat.id}>
                    {cat.name}
                  </option>
                ))}
              </select>
              {errors.categoryId && <p className="text-xs text-[var(--color-danger)]">{errors.categoryId.message}</p>}
            </div>

            {/* Date */}
            <div className="space-y-1.5">
              <label className="block text-sm font-medium text-[var(--color-text-secondary)]">
                Date <span className="text-[var(--color-danger)]">*</span>
              </label>
              <input
                {...register('txDate', { required: 'La date est requise' })}
                type="date"
                className={`w-full px-4 py-2 bg-[var(--color-bg-input)] border rounded-lg text-[var(--color-text-primary)] focus:outline-none focus:border-[var(--color-primary)] transition-colors [color-scheme:dark] ${
                  errors.txDate ? 'border-[var(--color-danger)]' : 'border-[var(--color-border-subtle)]'
                }`}
              />
              {errors.txDate && <p className="text-xs text-[var(--color-danger)]">{errors.txDate.message}</p>}
            </div>

            {/* Description */}
            <div className="space-y-1.5">
              <label className="block text-sm font-medium text-[var(--color-text-secondary)]">
                Description (optionnelle)
              </label>
              <textarea
                {...register('description')}
                rows={3}
                placeholder="Détails supplémentaires..."
                className="w-full px-4 py-2 bg-[var(--color-bg-input)] border border-[var(--color-border-subtle)] rounded-lg text-[var(--color-text-primary)] focus:outline-none focus:border-[var(--color-primary)] transition-colors resize-none"
              />
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
            form="transaction-form"
            disabled={isSubmitting}
            className="px-4 py-2 rounded-lg bg-[var(--color-primary)] text-[var(--color-bg-base)] font-medium hover:bg-[var(--color-primary-hover)] transition-colors disabled:opacity-50 flex items-center"
          >
            {isSubmitting ? 'Enregistrement...' : 'Enregistrer la transaction'}
          </button>
        </div>

      </div>
    </div>
  );
};
