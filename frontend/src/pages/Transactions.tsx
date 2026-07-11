import React, {useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { SearchX } from 'lucide-react';
import { TopBar } from '../components/layout/TopBar';
import { TransactionFilters } from '../components/transactions/TransactionFilters';
import type { FilterState } from '../components/transactions/TransactionFilters';
import { TransactionTable } from '../components/transactions/TransactionTable';
import { TransactionForm } from '../components/transactions/TransactionForm';
import type { TransactionFormData } from '../components/transactions/TransactionForm';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { EmptyState } from '../components/ui/EmptyState';
import { createTransaction, updateTransaction, deleteTransaction } from '../api/transactionApi';
import type { Transaction } from '../types';
import { useAuth } from '../hooks/useAuth';
import { toCentimes } from '../utils/formatCurrency';
import { useTransactions } from '../hooks/api/useTransactions';
import { useCategories } from '../hooks/api/useCategories';
import { useMutation } from '../hooks/useMutation';

export const Transactions: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();

  const [filters, setFilters] = useState<FilterState>({});
  const [page, setPage] = useState(0);

  const [isFormOpen, setIsFormOpen] = useState(false);
  const [formMode, setFormMode] = useState<'create' | 'edit'>('create');
  const [transactionToEdit, setTransactionToEdit] = useState<Transaction | null>(null);

  const [deleteDialog, setDeleteDialog] = useState<{ isOpen: boolean; id: number | null }>({
    isOpen: false,
    id: null,
  });

  const { data, isLoading, error: fetchError, refetch } = useTransactions(filters, page);
  const transactions = data?.content || [];
  const totalPages = data?.totalPages || 0;
  const totalElements = data?.totalElements || 0;

  const { data: categoriesData } = useCategories();
  const categories = categoriesData ?? [];

  const { mutate: performSave, error: saveError } = useMutation(
    async (formData: any) => {
      if (formMode === 'create') {
        return await createTransaction(formData);
      } else if (formMode === 'edit' && transactionToEdit) {
        return await updateTransaction(transactionToEdit.id, formData);
      }
    },
    {
      onSuccess: () => {
        setIsFormOpen(false);
        refetch();
      }
    }
  );

  const { mutate: performDelete } = useMutation(
    async (id: number) => await deleteTransaction(id),
    {
      onSuccess: () => {
        setDeleteDialog({ isOpen: false, id: null });
        refetch();
      }
    }
  );

  const error = fetchError || saveError;

  const handleFilterChange = (newFilters: FilterState) => {
    setFilters(newFilters);
    setPage(0); // reset to page 0 on filter change
  };

  const handleResetFilters = () => {
    setFilters({});
    setPage(0);
  };

  const handleCreateSubmit = async (data: TransactionFormData) => {
    const amountInCentimes = toCentimes(data.amount);
    const requestData = {
      title: data.title,
      amount: amountInCentimes,
      type: data.type,
      categoryId: data.categoryId,
      txDate: data.txDate,
      description: data.description,
    };
    await performSave(requestData);
  };

  const handleEditClick = (transaction: Transaction) => {
    setTransactionToEdit(transaction);
    setFormMode('edit');
    setIsFormOpen(true);
  };

  const handleDeleteClick = (id: number) => {
    setDeleteDialog({ isOpen: true, id });
  };

  const confirmDelete = async () => {
    if (deleteDialog.id) {
      await performDelete(deleteDialog.id);
    }
  };

  const isStandard = user?.role === 'STANDARD';

  return (
    <div className="flex-1 flex flex-col h-full bg-bg-base">
      <TopBar
        title="Vos transactions"
        action={{
          label: 'Nouvelle transaction',
          onClick: () => {
            setFormMode('create');
            setTransactionToEdit(null);
            setIsFormOpen(true);
          }
        }}
      />

      <div className="flex-1 p-8 overflow-y-auto space-y-6">

        {/* Limit Warning for STANDARD users */}
        {isStandard && (
          <div className="flex items-center justify-between p-4 rounded-xl bg-[var(--color-bg-card)] border border-[var(--color-border-subtle)]">
            <div>
              <p className="text-sm text-[var(--color-text-secondary)]">Utilisation (Standard)</p>
              <p className="font-medium">
                Vous avez utilisé <span className="text-[var(--color-primary)]">{totalElements}</span> transactions sur 500
              </p>
            </div>
            <a href="#" className="text-sm text-[var(--color-primary)] hover:underline font-medium">
              Passer Premium →
            </a>
          </div>
        )}

        <TransactionFilters
          filters={filters}
          categories={categories}
          onChange={handleFilterChange}
          onReset={handleResetFilters}
        />

        {error && <div className="text-[var(--color-danger)] p-4 bg-[var(--color-danger)]/10 rounded-xl border border-[var(--color-danger)]/20">{error}</div>}

        {isLoading ? (
          <div className="py-12 text-center text-[var(--color-text-secondary)] animate-pulse">Chargement des transactions...</div>
        ) : transactions.length > 0 ? (
          <>
            <TransactionTable
              transactions={transactions}
              onView={(id) => navigate(`/transactions/${id}`)}
              onEdit={handleEditClick}
              onDelete={handleDeleteClick}
            />

            {/* Pagination Controls */}
            {totalPages > 1 && (
              <div className="flex items-center justify-between pt-4">
                <span className="text-sm text-[var(--color-text-secondary)]">
                  Page {page + 1} sur {totalPages}
                </span>
                <div className="flex gap-2">
                  <button
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={page === 0}
                    className="px-4 py-2 text-sm bg-[var(--color-bg-card)] border border-[var(--color-border-subtle)] rounded-lg hover:bg-[var(--color-border-subtle)] disabled:opacity-50 disabled:hover:bg-[var(--color-bg-card)] transition-colors"
                  >
                    Précédent
                  </button>
                  <button
                    onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                    disabled={page >= totalPages - 1}
                    className="px-4 py-2 text-sm bg-[var(--color-bg-card)] border border-[var(--color-border-subtle)] rounded-lg hover:bg-[var(--color-border-subtle)] disabled:opacity-50 disabled:hover:bg-[var(--color-bg-card)] transition-colors"
                  >
                    Suivant
                  </button>
                </div>
              </div>
            )}
          </>
        ) : (
          <EmptyState
            icon={SearchX}
            title="Aucune transaction trouvée"
            description="Essayez de modifier vos filtres ou ajoutez une nouvelle transaction."
            actionLabel="+ Nouvelle transaction"
            onAction={() => {
              setFormMode('create');
              setTransactionToEdit(null);
              setIsFormOpen(true);
            }}
          />
        )}
      </div>

      <TransactionForm
        isOpen={isFormOpen}
        mode={formMode}
        initialData={transactionToEdit}
        categories={categories}
        onClose={() => setIsFormOpen(false)}
        onSubmit={handleCreateSubmit}
      />

      <ConfirmDialog
        isOpen={deleteDialog.isOpen}
        title="Supprimer la transaction ?"
        message="Cette action est irréversible. Êtes-vous sûr de vouloir continuer ?"
        onConfirm={confirmDelete}
        onCancel={() => setDeleteDialog({ isOpen: false, id: null })}
      />
    </div>
  );
};
