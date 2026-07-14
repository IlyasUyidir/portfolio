import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TransactionForm } from './TransactionForm';
import type { Category, Transaction } from '../../types';

// Mock data
const mockCategories: Category[] = [
  { id: 1, name: 'Alimentation', type: 'DEPENSE', color: '#EF4444', isSystem: true },
  { id: 2, name: 'Salaire', type: 'REVENU', color: '#22C55E', isSystem: true },
  { id: 3, name: 'Loisirs', type: 'BOTH', color: '#888', isSystem: false },
];

const existingTransaction: Transaction = {
  id: 100,
  title: 'Courses Carrefour',
  amount: 50000, // 500.00 DH
  type: 'DEPENSE',
  category: mockCategories[0],
  txDate: '2026-05-10',
  createdAt: '2026-05-10T10:00:00Z',
};

describe('TransactionForm', () => {
  const onSubmit = vi.fn();
  const onClose = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('TransactionForm_whenClosed_shouldNotRender', () => {
    // Arrange
    render(
      <TransactionForm
        mode="create"
        categories={mockCategories}
        isOpen={false}
        onClose={onClose}
        onSubmit={onSubmit}
      />
    );

    // Act & Assert
    expect(screen.queryByText('Nouvelle transaction')).not.toBeInTheDocument();
  });

  it('TransactionForm_whenOpen_shouldShowForm', () => {
    // Arrange
    render(
      <TransactionForm
        mode="create"
        categories={mockCategories}
        isOpen={true}
        onClose={onClose}
        onSubmit={onSubmit}
      />
    );

    // Act & Assert
    expect(screen.getByText('Nouvelle transaction')).toBeInTheDocument();
  });

  it('TransactionForm_shouldFilterCategoriesBySelectedType', async () => {
    // Arrange
    render(
      <TransactionForm
        mode="create"
        categories={mockCategories}
        isOpen={true}
        onClose={onClose}
        onSubmit={onSubmit}
      />
    );

    // Act: Open the category dropdown
    
    // Assert: Check options
    // Default type is DEPENSE, so we expect Alimentation (DEPENSE) and Loisirs (BOTH)
    expect(screen.getByText('Alimentation')).toBeInTheDocument();
    expect(screen.getByText('Loisirs')).toBeInTheDocument();
    expect(screen.queryByText('Salaire')).not.toBeInTheDocument();
  });

  it('TransactionForm_whenTypeChangesToRevenu_shouldFilterCategoriesForRevenu', async () => {
    // Arrange
    const user = userEvent.setup();
    render(
      <TransactionForm
        mode="create"
        categories={mockCategories}
        isOpen={true}
        onClose={onClose}
        onSubmit={onSubmit}
      />
    );

    // Act: Click "Revenu" toggle
    await user.click(screen.getByText('Revenu'));

    // Assert: Check options
    // Now we expect Salaire (REVENU) and Loisirs (BOTH)
    expect(screen.getByText('Salaire')).toBeInTheDocument();
    expect(screen.getByText('Loisirs')).toBeInTheDocument();
    expect(screen.queryByText('Alimentation')).not.toBeInTheDocument();
  });

  it('TransactionForm_whenSubmittingWithMissingTitle_shouldShowError', async () => {
    // Arrange
    const user = userEvent.setup();
    render(
      <TransactionForm
        mode="create"
        categories={mockCategories}
        isOpen={true}
        onClose={onClose}
        onSubmit={onSubmit}
      />
    );

    // Act: Click submit without filling anything
    await user.click(screen.getByText('Enregistrer la transaction'));

    // Assert
    await waitFor(() => {
      expect(screen.getByText(/Le titre est requis/i)).toBeInTheDocument();
    });
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('TransactionForm_whenSubmittingWithMissingCategory_shouldShowError', async () => {
    // Arrange
    const user = userEvent.setup();
    render(
      <TransactionForm
        mode="create"
        categories={mockCategories}
        isOpen={true}
        onClose={onClose}
        onSubmit={onSubmit}
      />
    );

    // Act: Fill title and amount, but leave category at default (0)
    await user.type(screen.getByLabelText(/Titre/i), 'Test');
    await user.type(screen.getByLabelText(/Montant/i), '100');
    await user.click(screen.getByText('Enregistrer la transaction'));

    // Assert
    await waitFor(() => {
      expect(screen.getByText(/Veuillez sélectionner une catégorie/i)).toBeInTheDocument();
    });
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('TransactionForm_whenValidData_shouldCallOnSubmit', async () => {
    // Arrange
    const user = userEvent.setup();
    onSubmit.mockResolvedValue(undefined);
    render(
      <TransactionForm
        mode="create"
        categories={mockCategories}
        isOpen={true}
        onClose={onClose}
        onSubmit={onSubmit}
      />
    );

    // Act: Fill all required fields
    await user.type(screen.getByLabelText(/Titre/i), 'Salaire Mai');
    await user.type(screen.getByLabelText(/Montant/i), '25000');
    await user.click(screen.getByText('Revenu')); // Change to revenue
    await user.selectOptions(screen.getByLabelText(/Catégorie/i), '2'); // Salaire
    
    await user.click(screen.getByText('Enregistrer la transaction'));

    // Assert
    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({
        title: 'Salaire Mai',
        amount: '25000',
        type: 'REVENU',
        categoryId: 2
      }));
    });
  });

  it('TransactionForm_inEditMode_shouldPrePopulateFields', () => {
    // Arrange
    render(
      <TransactionForm
        mode="edit"
        initialData={existingTransaction}
        categories={mockCategories}
        isOpen={true}
        onClose={onClose}
        onSubmit={onSubmit}
      />
    );

    // Act & Assert
    expect(screen.getByText('Modifier la transaction')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Courses Carrefour')).toBeInTheDocument();
    expect(screen.getByDisplayValue('500.00')).toBeInTheDocument(); // fromCentimes(50000)
    expect(screen.getByDisplayValue('2026-05-10')).toBeInTheDocument();
    
    const select = screen.getByLabelText(/Catégorie/i) as HTMLSelectElement;
    expect(select.value).toBe('1'); // Alimentation id
  });

  it('TransactionForm_cancelButton_shouldCallOnClose', async () => {
    // Arrange
    const user = userEvent.setup();
    render(
      <TransactionForm
        mode="create"
        categories={mockCategories}
        isOpen={true}
        onClose={onClose}
        onSubmit={onSubmit}
      />
    );

    // Act
    await user.click(screen.getByText('Annuler'));

    // Assert
    expect(onClose).toHaveBeenCalled();
  });

  it('TransactionForm_amountWithInvalidFormat_shouldShowError', async () => {
    // Arrange
    const user = userEvent.setup();
    render(
      <TransactionForm
        mode="create"
        categories={mockCategories}
        isOpen={true}
        onClose={onClose}
        onSubmit={onSubmit}
      />
    );

    // Act: Type invalid amount
    await user.type(screen.getByLabelText(/Montant/i), 'abc');
    await user.click(screen.getByText('Enregistrer la transaction'));

    // Assert
    await waitFor(() => {
      expect(screen.getByText(/Format invalide/i)).toBeInTheDocument();
    });
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('TransactionForm_amountWithNegativeNumber_shouldShowError', async () => {
    // Arrange
    const user = userEvent.setup();
    render(
      <TransactionForm
        mode="create"
        categories={mockCategories}
        isOpen={true}
        onClose={onClose}
        onSubmit={onSubmit}
      />
    );

    // Act: Type zero amount
    await user.type(screen.getByLabelText(/Montant/i), '0');
    await user.click(screen.getByText('Enregistrer la transaction'));

    // Assert
    await waitFor(() => {
      expect(screen.getByText(/Le montant doit être supérieur à 0/i)).toBeInTheDocument();
    });
    expect(onSubmit).not.toHaveBeenCalled();
  });
});
