import apiClient from './apiClient';

export const downloadCsv = async (): Promise<Blob> => {
  const response = await apiClient.get('/export/csv', {
    responseType: 'blob',
  });
  return response.data;
};

export const downloadExcel = async (): Promise<Blob> => {
  const response = await apiClient.get('/export/excel', {
    responseType: 'blob',
  });
  return response.data;
};
