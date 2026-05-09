import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { TopBar } from '../components/layout/TopBar';
import { TransactionDetail } from '../components/transactions/TransactionDetail';
import { TransactionForm } from '../components/transactions/TransactionForm';
import type { TransactionFormData } from '../components/transactions/TransactionForm';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { getTransaction, updateTransaction, deleteTransaction } from '../api/transactionApi';
import { listCategories } from '../api/categoryApi';
import type { Transaction, Category } from '../types';
import { toCentimes } from '../utils/formatCurrency';

export const TransactionDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [transaction, setTransaction] = useState<Transaction | null>(null);
  const [categories, setCategories] = useState<Category[]>([]);
  
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [isFormOpen, setIsFormOpen] = useState(false);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);

  useEffect(() => {
    const fetchData = async () => {
      setIsLoading(true);
      setError(null);
      try {
        if (!id) throw new Error("ID de transaction manquant");
        const [txData, catData] = await Promise.all([
          getTransaction(Number(id)),
          listCategories()
        ]);
        setTransaction(txData);
        setCategories(catData);
      } catch (err: any) {
        setError(err.response?.data?.error ?? 'Erreur lors du chargement de la transaction');
      } finally {
        setIsLoading(false);
      }
    };
    fetchData();
  }, [id]);

  const handleEditSubmit = async (data: TransactionFormData) => {
    if (!transaction) return;
    try {
      const amountInCentimes = toCentimes(data.amount);
      const updatedTx = await updateTransaction(transaction.id, {
        title: data.title,
        amount: amountInCentimes,
        type: data.type,
        categoryId: data.categoryId,
        txDate: data.txDate,
        description: data.description,
      });
      setTransaction(updatedTx);
      setIsFormOpen(false);
    } catch (err: any) {
      setError(err.response?.data?.error ?? 'Erreur lors de la modification');
    }
  };

  const confirmDelete = async () => {
    if (!transaction) return;
    try {
      await deleteTransaction(transaction.id);
      navigate('/transactions');
    } catch (err: any) {
      setError(err.response?.data?.error ?? 'Erreur lors de la suppression');
      setIsDeleteDialogOpen(false);
    }
  };

  return (
    <div className="flex flex-col h-full overflow-y-auto">
      <TopBar title="Détail de transaction" />

      <div className="p-8 max-w-4xl mx-auto w-full space-y-6">
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
