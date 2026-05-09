import React from 'react';
import { Pencil, Trash2, Calendar, FileText, Tag, ArrowRightLeft, Clock } from 'lucide-react';
import type { Transaction } from '../../types';
import { formatCurrency } from '../../utils/formatCurrency';
import { formatDate } from '../../utils/formatDate';
import { Badge } from '../ui/Badge';

interface TransactionDetailProps {
  transaction: Transaction;
  onEdit: () => void;
  onArchive: () => void;
}

export const TransactionDetail: React.FC<TransactionDetailProps> = ({
  transaction,
  onEdit,
  onArchive,
}) => {
  const isRevenu = transaction.type === 'REVENU';

  return (
    <div className="bg-[var(--color-bg-card)] rounded-xl border border-[var(--color-border-subtle)] overflow-hidden">
      
      {/* Header */}
      <div className="p-6 border-b border-[var(--color-border-subtle)] flex items-start justify-between">
        <div className="flex items-center gap-4">
          <div className={`p-4 rounded-xl ${isRevenu ? 'bg-[var(--color-success)]/10 text-[var(--color-success)]' : 'bg-[var(--color-danger)]/10 text-[var(--color-danger)]'}`}>
            <ArrowRightLeft className="w-8 h-8" />
          </div>
          <div>
            <h2 className="text-2xl font-semibold text-[var(--color-text-primary)] mb-1">
              {transaction.title}
            </h2>
            <div className={`text-xl font-bold ${isRevenu ? 'text-[var(--color-success)]' : 'text-[var(--color-danger)]'}`}>
              {formatCurrency(isRevenu ? transaction.amount : -transaction.amount, true)}
            </div>
          </div>
        </div>
        <span
          className={`px-3 py-1 text-sm font-medium rounded-full ${
            isRevenu
              ? 'bg-[var(--color-success)]/10 text-[var(--color-success)]'
              : 'bg-[var(--color-danger)]/10 text-[var(--color-danger)]'
          }`}
        >
          {isRevenu ? 'Revenu' : 'Dépense'}
        </span>
      </div>

      {/* Details Grid */}
      <div className="p-6 grid grid-cols-1 md:grid-cols-2 gap-8">
        
        <div className="space-y-6">
          <div>
            <div className="flex items-center gap-2 text-sm text-[var(--color-text-secondary)] mb-1 uppercase tracking-wider font-medium">
              <Calendar className="w-4 h-4" /> Date
            </div>
            <div className="text-[var(--color-text-primary)] font-medium">
              {formatDate(transaction.txDate)}
            </div>
          </div>

          <div>
            <div className="flex items-center gap-2 text-sm text-[var(--color-text-secondary)] mb-1 uppercase tracking-wider font-medium">
              <Tag className="w-4 h-4" /> Catégorie
            </div>
            <div>
              <Badge label={transaction.category.name} color={transaction.category.color} />
            </div>
          </div>
        </div>

        <div className="space-y-6">
          <div>
            <div className="flex items-center gap-2 text-sm text-[var(--color-text-secondary)] mb-1 uppercase tracking-wider font-medium">
              <Clock className="w-4 h-4" /> Créé le
            </div>
            <div className="text-[var(--color-text-primary)] font-medium">
              {new Date(transaction.createdAt).toLocaleString('fr-MA')}
            </div>
          </div>

          {transaction.description && (
            <div>
              <div className="flex items-center gap-2 text-sm text-[var(--color-text-secondary)] mb-1 uppercase tracking-wider font-medium">
                <FileText className="w-4 h-4" /> Description
              </div>
              <div className="text-[var(--color-text-primary)] bg-[var(--color-bg-base)] p-4 rounded-lg border border-[var(--color-border-subtle)] text-sm">
                {transaction.description}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Footer Info & Actions */}
      <div className="p-6 bg-[var(--color-bg-base)]/50 border-t border-[var(--color-border-subtle)] flex flex-col sm:flex-row items-center justify-between gap-4">
        <p className="text-sm text-[var(--color-text-muted)]">
          Note: La suppression d'une transaction modifiera votre solde et vos statistiques.
        </p>
        <div className="flex gap-3 w-full sm:w-auto">
          <button
            onClick={onEdit}
            className="flex-1 sm:flex-none flex items-center justify-center gap-2 px-4 py-2 bg-[var(--color-primary)] text-[var(--color-bg-base)] font-medium rounded-lg hover:bg-[var(--color-primary-hover)] transition-colors"
          >
            <Pencil className="w-4 h-4" />
            Modifier
          </button>
          <button
            onClick={onArchive}
            className="flex-1 sm:flex-none flex items-center justify-center gap-2 px-4 py-2 bg-transparent border border-[var(--color-danger)] text-[var(--color-danger)] font-medium rounded-lg hover:bg-[var(--color-danger)]/10 transition-colors"
          >
            <Trash2 className="w-4 h-4" />
            Archiver
          </button>
        </div>
      </div>
    </div>
  );
};
