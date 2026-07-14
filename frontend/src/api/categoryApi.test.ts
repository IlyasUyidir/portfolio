import { describe, it, expect, vi, beforeEach } from 'vitest';
import apiClient from './apiClient';
import { listCategories, createCategory, updateCategory, deleteCategory } from './categoryApi';
import type { Category, CreateCategoryRequest } from '../types';

vi.mock('./apiClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  }
}));

describe('categoryApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const mockCategory: Category = {
    id: 1,
    name: 'Food',
    color: '#ff0000',
    type: 'DEPENSE',
    isSystem: true,
  };

  describe('listCategories', () => {
    it('should get from correct endpoint', async () => {
      (apiClient.get as any).mockResolvedValue({ data: [mockCategory] });

      const result = await listCategories();

      expect(apiClient.get).toHaveBeenCalledWith('/categories');
      expect(result).toEqual([mockCategory]);
    });

    it('should throw error on failure', async () => {
      (apiClient.get as any).mockRejectedValue(new Error('Network Error'));

      await expect(listCategories()).rejects.toThrow('Network Error');
    });
  });

  describe('createCategory', () => {
    it('should post to correct endpoint', async () => {
      const data: CreateCategoryRequest = { name: 'Food', color: '#ff0000', type: 'DEPENSE' };
      (apiClient.post as any).mockResolvedValue({ data: mockCategory });

      const result = await createCategory(data);

      expect(apiClient.post).toHaveBeenCalledWith('/categories', data);
      expect(result).toEqual(mockCategory);
    });
  });

  describe('updateCategory', () => {
    it('should put to correct endpoint', async () => {
      const data: CreateCategoryRequest = { name: 'Food2', color: '#00ff00', type: 'DEPENSE' };
      (apiClient.put as any).mockResolvedValue({ data: mockCategory });

      const result = await updateCategory(1, data);

      expect(apiClient.put).toHaveBeenCalledWith('/categories/1', data);
      expect(result).toEqual(mockCategory);
    });
  });

  describe('deleteCategory', () => {
    it('should call delete on correct endpoint', async () => {
      (apiClient.delete as any).mockResolvedValue({});

      await deleteCategory(1);

      expect(apiClient.delete).toHaveBeenCalledWith('/categories/1');
    });
  });
});
