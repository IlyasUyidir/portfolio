import { describe, it, expect, vi, beforeEach } from 'vitest';
import apiClient from './apiClient';
import { 
  listTransactions, 
  getTransaction, 
  createTransaction, 
  updateTransaction, 
  deleteTransaction 
} from './transactionApi';

vi.mock('./apiClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('transactionApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('listTransactions', () => {
    it('should get with correct params', async () => {
      // Arrange
      const mockParams = { page: 0, size: 10, type: 'DEPENSE' as const, categoryId: 5 };
      const mockResponse = { data: { content: [], totalElements: 0, totalPages: 0, number: 0, size: 10 } };
      vi.mocked(apiClient.get).mockResolvedValue(mockResponse);

      // Act
      await listTransactions(mockParams);

      // Assert
      expect(apiClient.get).toHaveBeenCalledWith('/transactions', { params: mockParams });
    });

    it('should handle undefined filters in params', async () => {
      // Arrange
      const mockParams = { page: 0, size: 10, type: undefined, categoryId: undefined };
      const mockResponse = { data: { content: [], totalElements: 0, totalPages: 0, number: 0, size: 10 } };
      vi.mocked(apiClient.get).mockResolvedValue(mockResponse);

      // Act
      await listTransactions(mockParams);

      // Assert
      expect(apiClient.get).toHaveBeenCalledWith('/transactions', { params: mockParams });
    });
  });

  describe('getTransaction', () => {
    it('should get correct endpoint', async () => {
      // Arrange
      const mockTx = { id: 100, title: 'Test' };
      vi.mocked(apiClient.get).mockResolvedValue({ data: mockTx });

      // Act
      const result = await getTransaction(100);

      // Assert
      expect(apiClient.get).toHaveBeenCalledWith('/transactions/100');
      expect(result).toEqual(mockTx);
    });
  });

  describe('createTransaction', () => {
    it('should post to correct endpoint', async () => {
      // Arrange
      const mockData = { 
        title: 'Courses', 
        amount: 50000, 
        type: 'DEPENSE' as const, 
        categoryId: 1, 
        txDate: '2026-05-10' 
      };
      vi.mocked(apiClient.post).mockResolvedValue({ data: { ...mockData, id: 1 } });

      // Act
      await createTransaction(mockData);

      // Assert
      expect(apiClient.post).toHaveBeenCalledWith('/transactions', mockData);
    });
  });

  describe('updateTransaction', () => {
    it('should put to correct endpoint', async () => {
      // Arrange
      const mockData = { 
        title: 'Updated', 
        amount: 60000, 
        type: 'DEPENSE' as const, 
        categoryId: 1, 
        txDate: '2026-05-10' 
      };
      vi.mocked(apiClient.put).mockResolvedValue({ data: { ...mockData, id: 100 } });

      // Act
      await updateTransaction(100, mockData);

      // Assert
      expect(apiClient.put).toHaveBeenCalledWith('/transactions/100', mockData);
    });
  });

  describe('deleteTransaction', () => {
    it('should delete correct endpoint', async () => {
      // Arrange
      vi.mocked(apiClient.delete).mockResolvedValue({});

      // Act
      await deleteTransaction(100);

      // Assert
      expect(apiClient.delete).toHaveBeenCalledWith('/transactions/100');
    });
  });
});
