import React, { useState } from 'react';
import { Trash2 } from 'lucide-react';
import type { Goal, GoalProgress } from '../../types';
import { formatCurrency } from '../../utils/formatCurrency';
import { formatDate } from '../../utils/formatDate';
import { ProgressBar } from '../ui/ProgressBar';
import { ConfirmDialog } from '../ui/ConfirmDialog';

interface GoalCardProps {
  goal: Goal;
  progress: GoalProgress;
  onContribute: (goalId: number, title: string) => void;
  onDelete: (goalId: number) => void;
}

export const GoalCard: React.FC<GoalCardProps> = ({ goal, progress, onContribute, onDelete }) => {
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);

  const renderStatusChip = () => {
    switch (goal.status) {
      case 'EN_COURS':
        return <span className="bg-info/20 text-info px-2 py-1 rounded text-xs font-semibold">EN COURS</span>;
      case 'ATTEINT':
        return <span className="bg-success/20 text-success px-2 py-1 rounded text-xs font-semibold">ATTEINT</span>;
      case 'EN_RETARD':
        return <span className="bg-danger/20 text-danger px-2 py-1 rounded text-xs font-semibold">EN RETARD</span>;
      default:
        return null;
    }
  };

  const getStatusForProgressBar = () => {
    if (goal.status === 'EN_RETARD') return 'CRITICAL';
    if (goal.status === 'ATTEINT') return 'NORMAL'; // Maybe success? but ProgressBar only has NORMAL, WARNING, CRITICAL. It maps to primary if NORMAL.
    return 'NORMAL';
  };

  const milestones = [
    { value: 25, reached: progress.milestones.twentyFive },
    { value: 50, reached: progress.milestones.fifty },
    { value: 75, reached: progress.milestones.seventyFive },
    { value: 100, reached: progress.milestones.hundred },
  ];

  return (
    <>
      <div className="bg-bg-card p-5 rounded-xl border border-border-subtle flex flex-col gap-4">
        <div className="flex justify-between items-start">
          <div>
            <h3 className="text-lg font-bold text-text-primary">{goal.title}</h3>
            <p className="text-sm text-text-secondary mt-1">Cible : {formatDate(goal.targetDate)}</p>
          </div>
          <div className="flex items-center gap-3">
            {renderStatusChip()}
            <button
              onClick={() => setIsDeleteDialogOpen(true)}
              className="p-2 text-text-secondary hover:text-danger hover:bg-danger/10 rounded-lg transition-colors"
              title="Supprimer l'objectif"
            >
              <Trash2 size={18} />
            </button>
          </div>
        </div>

        <div className="flex justify-between items-end text-sm">
          <span className="text-text-primary font-medium">{formatCurrency(goal.currentAmount * 100)}</span>
          <span className="text-text-secondary">sur {formatCurrency(goal.targetAmount * 100)}</span>
        </div>

        <div className="relative pt-1 pb-4">
          <ProgressBar percent={progress.progressPercentage} status={getStatusForProgressBar()} showLabel={true} />
          
          <div className="absolute top-6 left-0 right-0 flex justify-between px-[5%] -mt-1 pointer-events-none">
            {milestones.map((m) => (
              <div
                key={m.value}
                className={`w-3 h-3 rounded-full border-2 border-bg-card z-10 ${m.reached ? 'bg-primary' : 'bg-bg-input'}`}
                style={{ marginLeft: m.value === 100 ? 'auto' : 0 }}
                title={`${m.value}%`}
              />
            ))}
          </div>
        </div>

        {goal.status !== 'ATTEINT' && (
          <button
            onClick={() => onContribute(goal.id, goal.title)}
            className="w-full mt-2 py-2.5 rounded-lg bg-primary text-bg-base font-semibold hover:bg-primary-hover transition-colors"
          >
            Contribuer
          </button>
        )}
      </div>

      <ConfirmDialog
        isOpen={isDeleteDialogOpen}
        title="Supprimer l'objectif"
        message={`Êtes-vous sûr de vouloir supprimer l'objectif « ${goal.title} » ? Cette action est irréversible.`}
        onConfirm={() => {
          setIsDeleteDialogOpen(false);
          onDelete(goal.id);
        }}
        onCancel={() => setIsDeleteDialogOpen(false)}
      />
    </>
  );
};
