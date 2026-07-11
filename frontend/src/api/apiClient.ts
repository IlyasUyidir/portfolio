import axios from 'axios';

const apiClient = axios.create({
baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
});

// Global error handling: 401 → redirect to /login
apiClient.interceptors.response.use(
  (res) => res,
  (error) => {
    if (error.response?.status === 401) {
      // Note: We don't manually remove the token from localStorage anymore
      // The browser handles the cookie. We just need to ensure the UI state is updated.
      // This redirect will trigger a re-render/re-check in AuthContext if needed.
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default apiClient;
