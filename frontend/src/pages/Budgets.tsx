import React, { useEffect, useState, useMemo } from 'react';
import { Target } from 'lucide-react';
import { TopBar } from '../components/layout/TopBar';
import { BudgetCard } from '../components/budgets/BudgetCard';
import { BudgetForm } from '../components/budgets/BudgetForm';
import type { BudgetFormData } from '../components/budgets/BudgetForm';
import { AlertBanner } from '../components/ui/AlertBanner';
import { EmptyState } from '../components/ui/EmptyState';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { listBudgetsByMonth, createBudget, deleteBudget } from '../api/budgetApi';
import { listCategories } from '../api/categoryApi';
import type { BudgetProgress, Category, Budget } from '../types';
import { toCentimes, formatCurrency } from '../utils/formatCurrency';

export const Budgets: React.FC = () => {
  const [month, setMonth] = useState(() => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
  });
  
  const [budgets, setBudgets] = useState<BudgetProgress[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [isFormOpen, setIsFormOpen] = useState(false);
  const [budgetToEdit, setBudgetToEdit] = useState<Budget | null>(null);
  
  const [deleteDialog, setDeleteDialog] = useState<{ isOpen: boolean; id: number | null }>({
    isOpen: false,
    id: null
  });

  const fetchData = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [budgetsData, catsData] = await Promise.all([
        listBudgetsByMonth(month),
        listCategories()
      ]);
      setBudgets(budgetsData);
      
      // Only keep DEPENSE or BOTH categories for budgets
      setCategories(catsData.filter(c => c.type !== 'REVENU'));
    } catch (err: any) {
      setError(err.response?.data?.error || 'Erreur lors du chargement des données');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [month]);

  const handleCreateOrUpdate = async (data: BudgetFormData) => {
    try {
      const [year, m] = data.monthString.split('-');
      await createBudget({
        categoryId: data.categoryId,
        budgetYear: parseInt(year, 10),
        budgetMonth: parseInt(m, 10),
        limitAmount: toCentimes(data.limitAmount),
        alertThreshold: data.alertThreshold
      });
      setIsFormOpen(false);
      fetchData();
    } catch (err: any) {
      alert(err.response?.data?.error || "Erreur lors de l'enregistrement");
    }
  };

  const confirmDelete = async () => {
    if (deleteDialog.id) {
      try {
        await deleteBudget(deleteDialog.id);
        setDeleteDialog({ isOpen: false, id: null });
        fetchData();
      } catch (err: any) {
        alert(err.response?.data?.error || "Erreur lors de la suppression");
      }
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
    <div className="flex flex-col h-full overflow-y-auto">
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

      <div className="p-8 max-w-7xl mx-auto w-full space-y-8">
        
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
