import React from 'react';
import { Eye, Pencil, Trash2 } from 'lucide-react';
import type { Transaction } from '../../types';
import { formatCurrency } from '../../utils/formatCurrency';
import { formatDate } from '../../utils/formatDate';
import { Badge } from '../ui/Badge';

interface TransactionTableProps {
  transactions: Transaction[];
  onView: (id: number) => void;
  onEdit: (transaction: Transaction) => void;
  onDelete: (id: number) => void;
}

export const TransactionTable: React.FC<TransactionTableProps> = ({
  transactions,
  onView,
  onEdit,
  onDelete,
}) => {
  if (transactions.length === 0) {
    return null;
  }

  return (
    <div className="overflow-x-auto rounded-xl border border-[var(--color-border-subtle)] bg-[var(--color-bg-card)]">
      <table className="w-full text-left border-collapse">
        <thead>
          <tr className="border-b border-[var(--color-border-subtle)] text-xs font-medium text-[var(--color-text-muted)] uppercase tracking-wider bg-[var(--color-bg-base)]/50">
            <th className="p-4 py-3">Date ↑</th>
            <th className="p-4 py-3">Titre</th>
            <th className="p-4 py-3">Catégorie</th>
            <th className="p-4 py-3">Type</th>
            <th className="p-4 py-3 text-right">Montant</th>
            <th className="p-4 py-3 text-right">Actions</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-[var(--color-border-subtle)]">
          {transactions.map((tx) => (
            <tr key={tx.id} className="hover:bg-[var(--color-bg-base)]/30 transition-colors">
              <td className="p-4 whitespace-nowrap text-sm text-[var(--color-text-secondary)]">
                {formatDate(tx.txDate)}
              </td>
              <td className="p-4 text-sm font-medium text-[var(--color-text-primary)]">
                {tx.title}
              </td>
              <td className="p-4 whitespace-nowrap">
                <Badge label={tx.category.name} color={tx.category.color} />
              </td>
              <td className="p-4 whitespace-nowrap">
                <span
                  className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${
                    tx.type === 'REVENU'
                      ? 'bg-[var(--color-success)]/10 text-[var(--color-success)]'
                      : 'bg-[var(--color-danger)]/10 text-[var(--color-danger)]'
                  }`}
                >
                  {tx.type === 'REVENU' ? 'Revenu' : 'Dépense'}
                </span>
              </td>
              <td
                className={`p-4 whitespace-nowrap text-right font-medium ${
                  tx.type === 'REVENU' ? 'text-[var(--color-success)]' : 'text-[var(--color-danger)]'
                }`}
              >
                {formatCurrency(tx.type === 'DEPENSE' ? -tx.amount : tx.amount, true)}
              </td>
              <td className="p-4 whitespace-nowrap text-right">
                <div className="flex items-center justify-end gap-2 text-[var(--color-text-muted)]">
                  <button
                    onClick={() => onView(tx.id)}
                    className="p-1.5 hover:text-[var(--color-primary)] hover:bg-[var(--color-primary)]/10 rounded-md transition-colors"
                    title="Voir les détails"
                  >
                    <Eye className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => onEdit(tx)}
                    className="p-1.5 hover:text-[var(--color-info)] hover:bg-[var(--color-info)]/10 rounded-md transition-colors"
                    title="Modifier"
                  >
                    <Pencil className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => onDelete(tx.id)}
                    className="p-1.5 hover:text-[var(--color-danger)] hover:bg-[var(--color-danger)]/10 rounded-md transition-colors"
                    title="Supprimer"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};
