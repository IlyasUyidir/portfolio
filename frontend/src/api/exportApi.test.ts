import { describe, it, expect, vi, beforeEach } from 'vitest';
import apiClient from './apiClient';
import { downloadCsv, downloadExcel } from './exportApi';

vi.mock('./apiClient', () => ({
  default: {
    get: vi.fn(),
  }
}));

describe('exportApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const mockBlob = new Blob(['test'], { type: 'text/csv' });

  describe('downloadCsv', () => {
    it('should get from correct endpoint with blob responseType', async () => {
      (apiClient.get as any).mockResolvedValue({ data: mockBlob });

      const result = await downloadCsv();

      expect(apiClient.get).toHaveBeenCalledWith('/export/csv', { responseType: 'blob' });
      expect(result).toEqual(mockBlob);
    });

    it('should throw error on failure', async () => {
      (apiClient.get as any).mockRejectedValue(new Error('Network Error'));

      await expect(downloadCsv()).rejects.toThrow('Network Error');
    });
  });

  describe('downloadExcel', () => {
    it('should get from correct endpoint with blob responseType', async () => {
      const mockExcelBlob = new Blob(['test'], { type: 'application/vnd.ms-excel' });
      (apiClient.get as any).mockResolvedValue({ data: mockExcelBlob });

      const result = await downloadExcel();

      expect(apiClient.get).toHaveBeenCalledWith('/export/excel', { responseType: 'blob' });
      expect(result).toEqual(mockExcelBlob);
    });
  });
});
