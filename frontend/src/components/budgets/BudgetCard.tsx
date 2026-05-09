import React from 'react';
import { Pencil, Trash2 } from 'lucide-react';
import type { BudgetProgress } from '../../types';
import { formatCurrency } from '../../utils/formatCurrency';
import { ProgressBar } from '../ui/ProgressBar';

interface BudgetCardProps {
  progress: BudgetProgress;
  onEdit: (budgetId: number) => void;
  onDelete: (budgetId: number) => void;
}

export const BudgetCard: React.FC<BudgetCardProps> = ({ progress, onEdit, onDelete }) => {
  const { budget, spentAmount, remainingAmount, spentPercentage, alertStatus } = progress;
  const { category, limitAmount } = budget;

  // Status text map
  const statusText = {
    NORMAL: 'En cours',
    WARNING: `Alerte > ${budget.alertThreshold}%`,
    CRITICAL: 'Dépassé'
  };

  const statusBg = {
    NORMAL: 'bg-primary/10 text-primary',
    WARNING: 'bg-warning/10 text-warning',
    CRITICAL: 'bg-danger/10 text-danger'
  };

  return (
    <div className={`bg-bg-card border rounded-2xl p-6 transition-all ${
      alertStatus === 'CRITICAL' ? 'border-danger/30 shadow-[0_0_15px_rgba(239,68,68,0.1)]' :
      alertStatus === 'WARNING' ? 'border-warning/30 shadow-[0_0_15px_rgba(245,158,11,0.1)]' :
      'border-border-subtle hover:border-border-focus'
    }`}>
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <div 
            className="w-10 h-10 rounded-xl flex items-center justify-center text-white font-bold text-lg"
            style={{ backgroundColor: category.color }}
          >
            {category.name.charAt(0).toUpperCase()}
          </div>
          <div>
            <h3 className="font-bold text-text-primary text-lg">{category.name}</h3>
            <span className={`inline-block px-2 py-0.5 rounded text-xs font-semibold mt-1 ${statusBg[alertStatus]}`}>
              {statusText[alertStatus]}
            </span>
          </div>
        </div>
        
        {/* Actions */}
        <div className="flex items-center gap-2">
          <button 
            onClick={() => onEdit(budget.id)}
            className="p-2 text-text-secondary hover:text-primary hover:bg-bg-input rounded-lg transition-colors"
            title="Modifier"
          >
            <Pencil className="w-4 h-4" />
          </button>
          <button 
            onClick={() => onDelete(budget.id)}
            className="p-2 text-text-secondary hover:text-danger hover:bg-danger/10 rounded-lg transition-colors"
            title="Supprimer"
          >
            <Trash2 className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-3 gap-4 mb-6">
        <div>
          <p className="text-xs text-text-secondary uppercase tracking-wider mb-1">Dépensé</p>
          <p className="font-bold text-text-primary">{formatCurrency(spentAmount)}</p>
        </div>
        <div>
          <p className="text-xs text-text-secondary uppercase tracking-wider mb-1">Limite</p>
          <p className="font-bold text-text-primary">{formatCurrency(limitAmount)}</p>
        </div>
        <div>
          <p className="text-xs text-text-secondary uppercase tracking-wider mb-1">Reste</p>
          <p className={`font-bold ${alertStatus === 'CRITICAL' ? 'text-danger' : 'text-success'}`}>
            {formatCurrency(remainingAmount)}
          </p>
        </div>
      </div>

      {/* Progress */}
      <div className="mt-4">
        <div className="flex justify-between items-center mb-2">
          <span className="text-sm font-medium text-text-secondary">Progression</span>
        </div>
        <ProgressBar percent={spentPercentage} status={alertStatus} showLabel={true} />
      </div>
    </div>
  );
};
