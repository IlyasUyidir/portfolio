import React, { useState, useEffect } from 'react';
import { useAuth } from '../hooks/useAuth';
import { listCategories, createCategory, updateCategory, deleteCategory } from '../api/categoryApi';
import type { Category, CreateCategoryRequest } from '../types';
import { CategoryList } from '../components/categories/CategoryList';
import { CategoryForm } from '../components/categories/CategoryForm';
import { TopBar } from '../components/layout/TopBar';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { AlertBanner } from '../components/ui/AlertBanner';

export const Categories: React.FC = () => {
  const { isPremium } = useAuth();
  const [categories, setCategories] = useState<Category[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingCategory, setEditingCategory] = useState<Category | null>(null);

  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [deletingCategory, setDeletingCategory] = useState<Category | null>(null);

  const fetchCategories = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await listCategories();
      setCategories(data);
    } catch (err: any) {
      setError(err.response?.data?.error ?? 'Erreur lors du chargement des catégories');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchCategories();
  }, []);

  const customCategoriesCount = categories.filter((c) => !c.isSystem).length;
  const isLimitReached = !isPremium && customCategoriesCount >= 10;

  const handleOpenForm = (category?: Category) => {
    if (category) {
      setEditingCategory(category);
    } else {
      setEditingCategory(null);
    }
    setIsFormOpen(true);
  };

  const handleCloseForm = () => {
    setIsFormOpen(false);
    setEditingCategory(null);
  };

  const handleSubmitForm = async (data: CreateCategoryRequest) => {
    try {
      if (editingCategory) {
        await updateCategory(editingCategory.id, data);
      } else {
        await createCategory(data);
      }
      handleCloseForm();
      fetchCategories();
    } catch (err: any) {
      setError(err.response?.data?.error ?? 'Erreur lors de la sauvegarde de la catégorie');
    }
  };

  const handleOpenDeleteDialog = (category: Category) => {
    setDeletingCategory(category);
    setIsDeleteDialogOpen(true);
  };

  const handleConfirmDelete = async () => {
    if (!deletingCategory) return;
    try {
      await deleteCategory(deletingCategory.id);
      setIsDeleteDialogOpen(false);
      setDeletingCategory(null);
      fetchCategories();
    } catch (err: any) {
      setError(err.response?.data?.error ?? 'Erreur lors de la suppression de la catégorie');
    }
  };

  return (
    <div className="flex-1 flex flex-col h-full bg-bg-base">
      <TopBar
        title="Catégories"
        action={{
          label: 'Nouvelle catégorie',
          onClick: () => handleOpenForm(),
        }}
        actionDisabled={isLimitReached}
      />

      <div className="flex-1 p-8 overflow-y-auto space-y-6">
        {error && (
          <div className="bg-danger/10 border border-danger text-danger p-4 rounded-xl">
            {error}
          </div>
        )}

        {isLimitReached && (
          <AlertBanner
            message="Vous avez atteint la limite de 10 catégories personnalisées (Standard). Passez à Premium pour en créer davantage."
            severity="warning"
            onDismiss={() => {}} // Could be implemented, but a static warning is fine
          />
        )}

        {isLoading ? (
          <div className="text-text-secondary">Chargement...</div>
        ) : (
          <CategoryList
            categories={categories}
            onEdit={handleOpenForm}
            onDelete={handleOpenDeleteDialog}
          />
        )}
      </div>

      <CategoryForm
        isOpen={isFormOpen}
        initialData={editingCategory}
        onClose={handleCloseForm}
        onSubmit={handleSubmitForm}
      />

      <ConfirmDialog
        isOpen={isDeleteDialogOpen}
        title="Supprimer la catégorie"
        message={`Êtes-vous sûr de vouloir supprimer la catégorie "${deletingCategory?.name}" ? Cette action est irréversible.`}
        onConfirm={handleConfirmDelete}
        onCancel={() => {
          setIsDeleteDialogOpen(false);
          setDeletingCategory(null);
        }}
      />
    </div>
  );
};
