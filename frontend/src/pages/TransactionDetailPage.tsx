import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { TopBar } from '../components/layout/TopBar';
import { TransactionDetail } from '../components/transactions/TransactionDetail';
import { TransactionForm } from '../components/transactions/TransactionForm';
import type { TransactionFormData } from '../components/transactions/TransactionForm';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { getTransaction, updateTransaction, deleteTransaction } from '../api/transactionApi';
import { toCentimes } from '../utils/formatCurrency';
import { useQuery } from '../hooks/useQuery';
import { useMutation } from '../hooks/useMutation';
import { useCategories } from '../hooks/api/useCategories';

export const TransactionDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [isFormOpen, setIsFormOpen] = useState(false);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);

  const { data: transaction, isLoading: isTxLoading, error: txError, refetch } = useQuery(
    async () => {
      if (!id) throw new Error("ID de transaction manquant");
      return await getTransaction(Number(id));
    },
    [id]
  );

  const { data: categoriesData } = useCategories();
  const categories = categoriesData ?? [];

  const { mutate: performUpdate, isLoading: isUpdating, error: updateError } = useMutation(
    async (formData: TransactionFormData) => {
      if (!transaction) throw new Error("Transaction non chargée");
      const amountInCentimes = toCentimes(formData.amount);
      return await updateTransaction(transaction.id, {
        title: formData.title,
        amount: amountInCentimes,
        type: formData.type,
        categoryId: formData.categoryId,
        txDate: formData.txDate,
        description: formData.description,
      });
    },
    {
      onSuccess: () => {
        setIsFormOpen(false);
        refetch();
      }
    }
  );

  const { mutate: performDelete, isLoading: isDeleting, error: deleteError } = useMutation(
    async (transactionId: number) => {
      return await deleteTransaction(transactionId);
    },
    {
      onSuccess: () => navigate('/transactions')
    }
  );

  const isLoading = isTxLoading || isUpdating || isDeleting;
  const error = txError || updateError || deleteError;

  const handleEditSubmit = async (data: TransactionFormData) => {
    await performUpdate(data);
  };

  const confirmDelete = async () => {
    if (transaction) {
      await performDelete(transaction.id);
    }
  };

  return (
    <div className="flex-1 flex flex-col h-full bg-bg-base">
      <TopBar title="Détail de transaction" />

      <div className="flex-1 p-8 overflow-y-auto space-y-6">
        <button
          onClick={() => navigate('/transactions')}
          className="flex items-center gap-2 text-sm text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] transition-colors w-fit"
        >
          <ArrowLeft className="w-4 h-4" />
          Retour aux transactions
        </button>

        {error && <div className="text-[var(--color-danger)] p-4 bg-[var(--color-danger)]/10 rounded-xl border border-[var(--color-danger)]/20">{error}</div>}

        {isLoading ? (
          <div className="py-12 text-center text-[var(--color-text-secondary)] animate-pulse">Chargement des détails...</div>
        ) : transaction ? (
          <TransactionDetail
            transaction={transaction}
            onEdit={() => setIsFormOpen(true)}
            onArchive={() => setIsDeleteDialogOpen(true)}
          />
        ) : null}
      </div>

      {transaction && (
        <TransactionForm
          isOpen={isFormOpen}
          mode="edit"
          initialData={transaction}
          categories={categories}
          onClose={() => setIsFormOpen(false)}
          onSubmit={handleEditSubmit}
        />
      )}

      <ConfirmDialog
        isOpen={isDeleteDialogOpen}
        title="Archiver la transaction ?"
        message="Cette action est irréversible. Êtes-vous sûr de vouloir continuer ?"
        onConfirm={confirmDelete}
        onCancel={() => setIsDeleteDialogOpen(false)}
      />
    </div>
  );
};
