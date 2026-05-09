import React from 'react';
import { Plus } from 'lucide-react';

interface EmptyGoalSlotProps {
  onClick: () => void;
}

export const EmptyGoalSlot: React.FC<EmptyGoalSlotProps> = ({ onClick }) => {
  return (
    <button
      onClick={onClick}
      className="w-full h-full min-h-[250px] bg-bg-card/50 border-2 border-dashed border-border-subtle hover:border-primary/50 hover:bg-bg-card transition-all rounded-xl p-6 flex flex-col items-center justify-center text-center group"
    >
      <div className="bg-bg-input group-hover:bg-primary/10 p-4 rounded-full mb-4 text-text-secondary group-hover:text-primary transition-colors">
        <Plus size={32} />
      </div>
      <h3 className="text-lg font-bold text-text-primary mb-1">
        Créer mon objectif gratuit
      </h3>
      <p className="text-sm text-text-secondary max-w-[200px]">
        Définissez une cible et commencez à épargner dès aujourd'hui.
      </p>
    </button>
  );
};
