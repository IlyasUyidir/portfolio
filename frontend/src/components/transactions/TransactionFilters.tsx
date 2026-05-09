import React from 'react';
import type { Category, TransactionType } from '../../types';

export interface FilterState {
  startDate?: string;
  endDate?: string;
  type?: TransactionType | '';
  categoryId?: number | '';
}

interface TransactionFiltersProps {
  filters: FilterState;
  categories: Category[];
  onChange: (filters: FilterState) => void;
  onReset: () => void;
}

export const TransactionFilters: React.FC<TransactionFiltersProps> = ({
  filters,
  categories,
  onChange,
  onReset
}) => {
  const handleChange = (e: React.ChangeEvent<HTMLSelectElement | HTMLInputElement>) => {
    const { name, value } = e.target;
    onChange({
      ...filters,
      [name]: value === '' ? '' : name === 'categoryId' ? Number(value) : value,
    });
  };

  return (
    <div className="flex flex-wrap items-end gap-4 p-4 mb-6 bg-[var(--color-bg-card)] border border-[var(--color-border-subtle)] rounded-xl">
      <div className="flex flex-col gap-1.5 flex-1 min-w-[150px]">
        <label className="text-sm text-[var(--color-text-secondary)]">Type</label>
        <select
          name="type"
          value={filters.type || ''}
          onChange={handleChange}
          className="px-3 py-2 bg-[var(--color-bg-input)] border border-[var(--color-border-subtle)] rounded-lg text-[var(--color-text-primary)] focus:outline-none focus:border-[var(--color-primary)]"
        >
          <option value="">Tous les types</option>
          <option value="DEPENSE">Dépense</option>
          <option value="REVENU">Revenu</option>
        </select>
      </div>

      <div className="flex flex-col gap-1.5 flex-1 min-w-[150px]">
        <label className="text-sm text-[var(--color-text-secondary)]">Catégorie</label>
        <select
          name="categoryId"
          value={filters.categoryId || ''}
          onChange={handleChange}
          className="px-3 py-2 bg-[var(--color-bg-input)] border border-[var(--color-border-subtle)] rounded-lg text-[var(--color-text-primary)] focus:outline-none focus:border-[var(--color-primary)]"
        >
          <option value="">Toutes catégories</option>
          {categories.map((cat) => (
            <option key={cat.id} value={cat.id}>
              {cat.name}
            </option>
          ))}
        </select>
      </div>

      <div className="flex flex-col gap-1.5 flex-1 min-w-[150px]">
        <label className="text-sm text-[var(--color-text-secondary)]">Date de début</label>
        <input
          type="date"
          name="startDate"
          value={filters.startDate || ''}
          onChange={handleChange}
          className="px-3 py-2 bg-[var(--color-bg-input)] border border-[var(--color-border-subtle)] rounded-lg text-[var(--color-text-primary)] focus:outline-none focus:border-[var(--color-primary)] [color-scheme:dark]"
        />
      </div>

      <div className="flex flex-col gap-1.5 flex-1 min-w-[150px]">
        <label className="text-sm text-[var(--color-text-secondary)]">Date de fin</label>
        <input
          type="date"
          name="endDate"
          value={filters.endDate || ''}
          onChange={handleChange}
          className="px-3 py-2 bg-[var(--color-bg-input)] border border-[var(--color-border-subtle)] rounded-lg text-[var(--color-text-primary)] focus:outline-none focus:border-[var(--color-primary)] [color-scheme:dark]"
        />
      </div>

      <div className="flex gap-2 h-[42px] mt-auto">
        <button
          onClick={onReset}
          className="px-4 py-2 bg-transparent border border-[var(--color-border-subtle)] rounded-lg text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] hover:bg-[var(--color-border-subtle)] transition-colors"
        >
          Réinitialiser
        </button>
      </div>
    </div>
  );
};
