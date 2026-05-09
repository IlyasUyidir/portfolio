import React from 'react';
import { useForm } from 'react-hook-form';

interface ContributeModalProps {
  goalId: number;
  goalTitle: string;
  isOpen: boolean;
  onSuccess: (amount: string | number) => void;
  onClose: () => void;
}

interface ContributeFormValues {
  amount: number;
}

export const ContributeModal: React.FC<ContributeModalProps> = ({ goalTitle, isOpen, onSuccess, onClose }) => {
  const { register, handleSubmit, formState: { errors }, reset } = useForm<ContributeFormValues>();

  if (!isOpen) return null;

  const onSubmit = (data: ContributeFormValues) => {
    onSuccess(data.amount);
    reset();
  };

  const handleClose = () => {
    reset();
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm">
      <div className="bg-bg-card w-full max-w-sm rounded-xl p-6 shadow-xl border border-border-subtle">
        <h3 className="text-lg font-semibold text-text-primary mb-1">Contribuer</h3>
        <p className="text-text-secondary text-sm mb-6">Ajouter des fonds à « {goalTitle} »</p>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-text-primary mb-1">
              Montant (DH) *
            </label>
            <input
              type="number"
              step="0.01"
              className="w-full bg-bg-input border border-border-subtle text-text-primary rounded-lg px-4 py-2 focus:outline-none focus:border-primary"
              {...register('amount', {
                required: 'Le montant est requis',
                min: { value: 0.01, message: 'Le montant doit être positif' }
              })}
            />
            {errors.amount && <p className="text-danger text-sm mt-1">{errors.amount.message}</p>}
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={handleClose}
              className="px-4 py-2 rounded-lg text-text-secondary hover:bg-border-subtle hover:text-text-primary transition-colors"
            >
              Annuler
            </button>
            <button
              type="submit"
              className="px-4 py-2 rounded-lg bg-primary text-bg-base font-semibold hover:bg-primary-hover transition-colors"
            >
              Ajouter
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
