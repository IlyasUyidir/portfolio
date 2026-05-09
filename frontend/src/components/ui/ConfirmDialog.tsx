import React from 'react';

interface ConfirmDialogProps {
  isOpen: boolean;
  title: string;
  message: string;
  onConfirm: () => void;
  onCancel: () => void;
}

export const ConfirmDialog: React.FC<ConfirmDialogProps> = ({ isOpen, title, message, onConfirm, onCancel }) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm">
      <div className="bg-[var(--color-bg-card)] w-full max-w-sm rounded-xl p-6 shadow-xl border border-[var(--color-border-subtle)]">
        <h3 className="text-lg font-semibold text-[var(--color-text-primary)] mb-2">{title}</h3>
        <p className="text-[var(--color-text-secondary)] mb-6">{message}</p>
        
        <div className="flex justify-end gap-3">
          <button
            onClick={onCancel}
            className="px-4 py-2 rounded-lg text-[var(--color-text-secondary)] hover:bg-[var(--color-border-subtle)] hover:text-[var(--color-text-primary)] transition-colors"
          >
            Annuler
          </button>
          <button
            onClick={onConfirm}
            className="px-4 py-2 rounded-lg bg-[var(--color-danger)] text-white font-medium hover:opacity-90 transition-opacity"
          >
            Confirmer
          </button>
        </div>
      </div>
    </div>
  );
};
