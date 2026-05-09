import React from 'react';
import { Edit2, Trash2 } from 'lucide-react';
import type { Category } from '../../types';

interface CategoryListProps {
  categories: Category[];
  onEdit: (category: Category) => void;
  onDelete: (category: Category) => void;
}

export const CategoryList: React.FC<CategoryListProps> = ({ categories, onEdit, onDelete }) => {
  return (
    <div className="bg-bg-card border border-border-subtle rounded-xl overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="border-b border-border-subtle bg-bg-base text-text-secondary text-sm">
              <th className="py-4 px-6 font-medium">Nom</th>
              <th className="py-4 px-6 font-medium">Type</th>
              <th className="py-4 px-6 font-medium text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border-subtle">
            {categories.map((category) => (
              <tr key={category.id} className="hover:bg-bg-input/50 transition-colors">
                <td className="py-4 px-6">
                  <div className="flex items-center gap-3">
                    <div
                      className="w-4 h-4 rounded-full flex-shrink-0"
                      style={{ backgroundColor: category.color }}
                    />
                    <span className="font-medium text-text-primary">{category.name}</span>
                    {category.isSystem && (
                      <span className="ml-2 px-2 py-0.5 text-xs rounded-full bg-border-subtle text-text-secondary">
                        Système
                      </span>
                    )}
                  </div>
                </td>
                <td className="py-4 px-6">
                  <span className="text-text-secondary text-sm">
                    {category.type === 'BOTH'
                      ? 'Dépense et Revenu'
                      : category.type === 'DEPENSE'
                      ? 'Dépense'
                      : 'Revenu'}
                  </span>
                </td>
                <td className="py-4 px-6 text-right">
                  {!category.isSystem ? (
                    <div className="flex items-center justify-end gap-2">
                      <button
                        onClick={() => onEdit(category)}
                        className="p-2 text-text-secondary hover:text-primary transition-colors rounded-lg hover:bg-bg-input"
                        title="Modifier"
                      >
                        <Edit2 className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => onDelete(category)}
                        className="p-2 text-text-secondary hover:text-danger transition-colors rounded-lg hover:bg-bg-input"
                        title="Supprimer"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  ) : (
                    <span className="text-sm text-text-muted italic">Lecture seule</span>
                  )}
                </td>
              </tr>
            ))}
            {categories.length === 0 && (
              <tr>
                <td colSpan={3} className="py-8 text-center text-text-secondary">
                  Aucune catégorie trouvée.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};
