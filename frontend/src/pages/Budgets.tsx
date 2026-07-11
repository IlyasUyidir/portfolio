import React, {useState, useMemo } from 'react';
import { Target } from 'lucide-react';
import { TopBar } from '../components/layout/TopBar';
import { BudgetCard } from '../components/budgets/BudgetCard';
import { BudgetForm } from '../components/budgets/BudgetForm';
import type { BudgetFormData } from '../components/budgets/BudgetForm';
import { AlertBanner } from '../components/ui/AlertBanner';
import { EmptyState } from '../components/ui/EmptyState';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { toCentimes, formatCurrency } from '../utils/formatCurrency';
import { useBudgets } from '../hooks/api/useBudgets';
import { useCategories } from '../hooks/api/useCategories';
import { useMutation } from '../hooks/useMutation';
import type { Budget } from '../types';
import { createBudget, deleteBudget } from '../api/budgetApi';

export const Budgets: React.FC = () => {
  const [month, setMonth] = useState(() => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
  });

  const [isFormOpen, setIsFormOpen] = useState(false);
  const [budgetToEdit, setBudgetToEdit] = useState<Budget | null>(null);

  const [deleteDialog, setDeleteDialog] = useState<{ isOpen: boolean; id: number | null }>({
    isOpen: false,
    id: null
  });

  const { data: budgetsData, isLoading, error: fetchError, refetch } = useBudgets(month);
  const budgets = budgetsData ?? [];
  const { data: allCategoriesData } = useCategories();
  const allCategories = allCategoriesData ?? [];
  const categories = useMemo(() => allCategories.filter(c => c.type !== 'REVENU'), [allCategories]);

  const { mutate: performSave, error: saveError } = useMutation(
    async (data: BudgetFormData) => {
      const [year, m] = data.monthString.split('-');
      return await createBudget({
        categoryId: data.categoryId,
        budgetYear: parseInt(year, 10),
        budgetMonth: parseInt(m, 10),
        limitAmount: toCentimes(data.limitAmount),
        alertThreshold: data.alertThreshold
      });
    },
    {
      onSuccess: () => {
        setIsFormOpen(false);
        refetch();
      }
    }
  );

  const { mutate: performDelete } = useMutation(
    async (id: number) => await deleteBudget(id),
    {
      onSuccess: () => {
        setDeleteDialog({ isOpen: false, id: null });
        refetch();
      }
    }
  );

  const error = fetchError || saveError;

  const handleCreateOrUpdate = async (data: BudgetFormData) => {
    await performSave(data);
  };

  const confirmDelete = async () => {
    if (deleteDialog.id) {
      await performDelete(deleteDialog.id);
    }
  };

  const handleEditClick = (id: number) => {
    const budgetProg = budgets.find(b => b.budget.id === id);
    if (budgetProg) {
      setBudgetToEdit(budgetProg.budget);
      setIsFormOpen(true);
    }
  };

  const summary = useMemo(() => {
    let totalAllocated = 0;
    let totalSpent = 0;
    budgets.forEach(b => {
      totalAllocated += b.budget.limitAmount;
      totalSpent += b.spentAmount;
    });
    const remaining = Math.max(0, totalAllocated - totalSpent);
    return { totalAllocated, totalSpent, remaining };
  }, [budgets]);

  const criticalAlerts = budgets.filter(b => b.alertStatus === 'CRITICAL');
  const warningAlerts = budgets.filter(b => b.alertStatus === 'WARNING');

  return (
    <div className="flex-1 flex flex-col h-full bg-bg-base">
      <TopBar 
        title="Budgets" 
        action={{
          label: 'Définir un budget',
          onClick: () => {
            setBudgetToEdit(null);
            setIsFormOpen(true);
          }
        }}
      />

      <div className="flex-1 p-8 overflow-y-auto space-y-8">
        
        {/* Month Selector */}
        <div className="flex justify-between items-center">
          <h2 className="text-xl font-bold text-text-primary">Gérer vos budgets</h2>
          <input 
            type="month" 
            value={month}
            onChange={(e) => setMonth(e.target.value)}
            className="bg-bg-input border border-border-subtle rounded-lg px-4 py-2 text-text-primary focus:outline-none focus:border-primary"
          />
        </div>

        {/* Alerts */}
        <div>
          {criticalAlerts.map(b => (
            <AlertBanner 
              key={`crit-${b.budget.id}`}
              severity="critical"
              message={`Alerte critique : Votre budget "${b.budget.category.name}" a dépassé la limite !`}
              onDismiss={() => {}}
            />
          ))}
          {warningAlerts.map(b => (
            <AlertBanner 
              key={`warn-${b.budget.id}`}
              severity="warning"
              message={`Attention : Votre budget "${b.budget.category.name}" a atteint ${b.spentPercentage}% de la limite.`}
              onDismiss={() => {}}
            />
          ))}
        </div>

        {/* Summary KPIs */}
        {!isLoading && budgets.length > 0 && (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
            <div className="bg-bg-card p-6 rounded-2xl border border-border-subtle">
              <p className="text-sm font-medium text-text-secondary uppercase tracking-wider mb-2">Total alloué</p>
              <p className="text-3xl font-bold text-text-primary">{formatCurrency(summary.totalAllocated)}</p>
            </div>
            <div className="bg-bg-card p-6 rounded-2xl border border-border-subtle">
              <p className="text-sm font-medium text-text-secondary uppercase tracking-wider mb-2">Total dépensé</p>
              <p className="text-3xl font-bold text-text-primary">{formatCurrency(summary.totalSpent)}</p>
            </div>
            <div className="bg-bg-card p-6 rounded-2xl border border-border-subtle">
              <p className="text-sm font-medium text-text-secondary uppercase tracking-wider mb-2">Reste global</p>
              <p className="text-3xl font-bold text-text-primary">{formatCurrency(summary.remaining)}</p>
            </div>
          </div>
        )}

        {/* Content */}
        {isLoading ? (
          <div className="py-12 text-center text-text-secondary animate-pulse">Chargement des budgets...</div>
        ) : error ? (
          <div className="text-danger p-4 bg-danger/10 rounded-xl border border-danger/20">{error}</div>
        ) : budgets.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {budgets.map(b => (
              <BudgetCard 
                key={b.budget.id} 
                progress={b} 
                onEdit={handleEditClick} 
                onDelete={(id) => setDeleteDialog({ isOpen: true, id })}
              />
            ))}
          </div>
        ) : (
          <EmptyState 
            icon={Target} 
            title="Aucun budget défini" 
            description="Fixez des limites de dépenses par catégorie pour mieux contrôler vos finances ce mois-ci."
            actionLabel="Définir un budget"
            onAction={() => {
              setBudgetToEdit(null);
              setIsFormOpen(true);
            }}
          />
        )}
      </div>

      {/* Forms & Dialogs */}
      {isFormOpen && (
        <BudgetForm 
          initialData={budgetToEdit}
          categories={categories}
          onSubmit={handleCreateOrUpdate}
          onCancel={() => setIsFormOpen(false)}
        />
      )}

      <ConfirmDialog 
        isOpen={deleteDialog.isOpen}
        title="Supprimer le budget ?"
        message="Êtes-vous sûr de vouloir supprimer ce budget ? Cette action est irréversible, mais n'affectera pas vos transactions existantes."
        onConfirm={confirmDelete}
        onCancel={() => setDeleteDialog({ isOpen: false, id: null })}
      />
    </div>
  );
};
