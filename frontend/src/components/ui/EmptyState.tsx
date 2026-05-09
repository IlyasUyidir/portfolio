import React from 'react';
import type { LucideIcon } from 'lucide-react';

interface EmptyStateProps {
  icon: LucideIcon;
  title: string;
  description: string;
  actionLabel?: string;
  onAction?: () => void;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon: Icon,
  title,
  description,
  actionLabel,
  onAction
}) => {
  return (
    <div className="flex flex-col items-center justify-center py-16 px-4 text-center border border-dashed border-[var(--color-border-subtle)] rounded-xl bg-[var(--color-bg-card)]">
      <div className="bg-[var(--color-bg-base)] p-4 rounded-full mb-4">
        <Icon className="w-8 h-8 text-[var(--color-text-muted)]" />
      </div>
      <h3 className="text-lg font-medium text-[var(--color-text-primary)] mb-2">{title}</h3>
      <p className="text-[var(--color-text-secondary)] max-w-sm mb-6">{description}</p>
      
      {actionLabel && onAction && (
        <button
          onClick={onAction}
          className="px-4 py-2 rounded-lg bg-[var(--color-primary)] text-[var(--color-bg-base)] font-medium hover:bg-[var(--color-primary-hover)] transition-colors"
        >
          {actionLabel}
        </button>
      )}
    </div>
  );
};
